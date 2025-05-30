package com.example.callerscreen

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import org.webrtc.*
import com.google.firebase.firestore.*


@SuppressLint("StaticFieldLeak")
object WebRTCManager {

    private lateinit var context: Context
    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private lateinit var localVideoSource: VideoSource
    private lateinit var localAudioSource: AudioSource
    private lateinit var localVideoTrack: VideoTrack
    private lateinit var localAudioTrack: AudioTrack
    private lateinit var localSurfaceView: SurfaceViewRenderer
    private lateinit var remoteSurfaceView: SurfaceViewRenderer
    private var isVideoEnabled = true
    private var isAudioEnabled = true
    private var isFrontCamera = true

    private lateinit var videoCapturer: VideoCapturer
    private lateinit var audioTrack: AudioTrack
    private var videoSource: VideoSource? = null

    private var peerConnection: PeerConnection? = null
    private var localStream: MediaStream? = null
    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
    )

    private lateinit var firestore: FirebaseFirestore
    private lateinit var roomRef: DocumentReference
    private var roomId: String = ""
    private val bufferedIceCandidates = mutableListOf<IceCandidate>()

    private val eglBase = EglBase.create()



    fun toggleVideo() {
        isVideoEnabled = !isVideoEnabled
        localVideoTrack.setEnabled(isVideoEnabled)
    }

    fun toggleAudio() {
        isAudioEnabled = !isAudioEnabled
        audioTrack.setEnabled(isAudioEnabled)
    }

    fun switchCamera() {
        if (videoCapturer is CameraVideoCapturer) {
            val capturer = videoCapturer as CameraVideoCapturer
            capturer.switchCamera(null)
            isFrontCamera = !isFrontCamera
        }
    }

    // Ensure these tracks are initialized during init/startLocalVideo
    fun setTracks(video: VideoTrack, audio: AudioTrack, capturer: VideoCapturer, source: VideoSource) {
       localVideoTrack = video
        audioTrack = audio
        videoCapturer = capturer
        videoSource = source
    }

    fun init(context: Context) {
        this.context = context
        firestore = FirebaseFirestore.getInstance()

        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions()
        )

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()
    }

    fun setSurfaceViews(local: SurfaceViewRenderer, remote: SurfaceViewRenderer) {
        localSurfaceView = local
        remoteSurfaceView = remote
        local.init(eglBase.eglBaseContext, null)
        remote.init(eglBase.eglBaseContext, null)
    }

    fun startLocalVideo() {
        val videoCapturer = createVideoCapturer() ?: return
        val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
        localVideoSource = peerConnectionFactory.createVideoSource(false)
        videoCapturer.initialize(surfaceTextureHelper, context, localVideoSource.capturerObserver)
        videoCapturer.startCapture(1280, 720, 30)

        localVideoTrack = peerConnectionFactory.createVideoTrack("LOCAL_VIDEO_TRACK", localVideoSource)
        localAudioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory.createAudioTrack("LOCAL_AUDIO_TRACK", localAudioSource)

        localStream = peerConnectionFactory.createLocalMediaStream("LOCAL_STREAM").apply {
            addTrack(localVideoTrack)
            addTrack(localAudioTrack)
        }

        localVideoTrack.addSink(localSurfaceView)
    }

    private fun createVideoCapturer(): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        return enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }?.let {
            enumerator.createCapturer(it, null)
        }
    }

    fun joinCall(roomId: String) {
        this.roomId = roomId
        roomRef = firestore.collection("calls").document(roomId)
        createPeerConnection()
        listenForOffer()
        listenForIceCandidates()
    }

    private fun createPeerConnection() {
        peerConnection = peerConnectionFactory.createPeerConnection(
            PeerConnection.RTCConfiguration(iceServers),
            object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate) {
                    roomRef.collection("candidates").add(
                        mapOf(
                            "sdpMid" to candidate.sdpMid,
                            "sdpMLineIndex" to candidate.sdpMLineIndex,
                            "candidate" to candidate.sdp
                        )
                    )
                }

                override fun onAddStream(stream: MediaStream) {
                    stream.videoTracks.firstOrNull()?.addSink(remoteSurfaceView)
                }

                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                    Log.d("WebRTC", "ICE connection state: $state")
                }

                override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                    Log.d("WebRTC", "Connection state: $newState")
                }

                fun onSetRemoteDescriptionSuccess() {
                    bufferedIceCandidates.forEach {
                        peerConnection?.addIceCandidate(it)
                    }
                    bufferedIceCandidates.clear()
                }

                override fun onSignalingChange(p0: PeerConnection.SignalingState) {}
                override fun onIceConnectionReceivingChange(p0: Boolean) {}
                override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState) {}
                override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>) {}
                override fun onRemoveStream(p0: MediaStream) {}
                override fun onDataChannel(p0: DataChannel) {}
                override fun onRenegotiationNeeded() {}
                override fun onTrack(transceiver: RtpTransceiver) {}
            }
        )

        localStream?.let {
            peerConnection?.addStream(it)
        }
    }

    private fun listenForOffer() {
        roomRef.addSnapshotListener { snapshot, _ ->
            val offer = snapshot?.getString("offer")
            val type = snapshot?.getString("type")
            if (offer != null && type == "offer") {
                val desc = SessionDescription(SessionDescription.Type.OFFER, offer)
                peerConnection?.setRemoteDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        createAnswer()
                    }

                    override fun onCreateSuccess(desc: SessionDescription?) {}
                    override fun onCreateFailure(error: String?) {}
                    override fun onSetFailure(error: String?) {
                        Log.e("WebRTC", "Failed to set remote description: $error")
                    }
                }, desc)
            }
        }
    }

    private fun createAnswer() {
        val constraints = MediaConstraints()
        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        roomRef.update(
                            mapOf(
                                "answer" to desc.description,
                                "type" to desc.type.canonicalForm()
                            )
                        )
                    }

                    override fun onSetFailure(error: String?) {}
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                }, desc)
            }

            override fun onCreateFailure(msg: String?) {}
            override fun onSetSuccess() {}
            override fun onSetFailure(msg: String?) {}
        }, constraints)
    }

    private fun listenForIceCandidates() {
        roomRef.collection("candidates").addSnapshotListener { snapshot, _ ->
            snapshot?.documentChanges?.forEach { change ->
                val data = change.document.data
                val candidate = IceCandidate(
                    data["sdpMid"] as? String ?: return@forEach,
                    (data["sdpMLineIndex"] as? Long)?.toInt() ?: return@forEach,
                    data["candidate"] as? String ?: return@forEach
                )
                if (peerConnection?.remoteDescription != null) {
                    peerConnection?.addIceCandidate(candidate)
                } else {
                    bufferedIceCandidates.add(candidate)
                }
            }
        }
    }

    fun endCall() {
        peerConnection?.close()
        peerConnection = null
        roomRef.delete()
        localVideoSource.dispose()
        localAudioSource.dispose()
        localSurfaceView.release()
        remoteSurfaceView.release()
    }
}

