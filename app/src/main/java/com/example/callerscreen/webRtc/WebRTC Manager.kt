package com.example.callerscreen.webRtc

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

    private var peerConnection: PeerConnection? = null
    private var localStream: MediaStream? = null
    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
    )

    private lateinit var firestore: FirebaseFirestore
    private lateinit var roomRef: DocumentReference
    private var roomId: String = ""

    fun init(context: Context) {
        WebRTCManager.context = context
        firestore = FirebaseFirestore.getInstance()

        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val encoderFactory = DefaultVideoEncoderFactory(
            EglBase.create().eglBaseContext, true, true
        )
        val decoderFactory = DefaultVideoDecoderFactory(EglBase.create().eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }

    fun setSurfaceViews(local: SurfaceViewRenderer, remote: SurfaceViewRenderer) {
        localSurfaceView = local
        remoteSurfaceView = remote
        local.init(EglBase.create().eglBaseContext, null)
        remote.init(EglBase.create().eglBaseContext, null)
    }

    fun startLocalVideo() {
        val videoCapturer = createVideoCapturer() ?: return
        val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", EglBase.create().eglBaseContext)
        localVideoSource = peerConnectionFactory.createVideoSource(false)
        videoCapturer.initialize(surfaceTextureHelper, context, localVideoSource.capturerObserver)
        videoCapturer.startCapture(1280, 720, 30)

        localVideoTrack = peerConnectionFactory.createVideoTrack("LOCAL_VIDEO_TRACK", localVideoSource)
        localVideoTrack.addSink(localSurfaceView)

        localAudioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory.createAudioTrack("LOCAL_AUDIO_TRACK", localAudioSource)

        localStream = peerConnectionFactory.createLocalMediaStream("LOCAL_STREAM")
        localStream?.addTrack(localVideoTrack)
        localStream?.addTrack(localAudioTrack)
    }

    private fun createVideoCapturer(): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        val deviceNames = enumerator.deviceNames
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        return null
    }

    fun joinCall(roomId: String) {
        WebRTCManager.roomId = roomId
        roomRef = firestore.collection("calls").document(roomId)
        createPeerConnection()
        listenForOffer()
        listenForIceCandidates()
    }

    private fun createPeerConnection() {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {

            override fun onIceCandidate(candidate: IceCandidate) {
                roomRef.collection("candidates").add(
                    hashMapOf(
                        "sdpMid" to candidate.sdpMid,
                        "sdpMLineIndex" to candidate.sdpMLineIndex,
                        "candidate" to candidate.sdp
                    )
                )
            }


            override fun onIceConnectionReceivingChange(receiving: Boolean) {}

            override fun onAddTrack(receiver: RtpReceiver, streams: Array<out MediaStream>) {}


            override fun onAddStream(stream: MediaStream) {
                if (stream.videoTracks.isNotEmpty()) {
                    stream.videoTracks[0].addSink(remoteSurfaceView)
                }
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                Log.d("WebRTC", "ICE connection state changed to $state")
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                Log.d("WebRTC", "Connection state: $newState")
            }

            override fun onSignalingChange(state: PeerConnection.SignalingState) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {}
            override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) {}
            override fun onRemoveStream(stream: MediaStream) {}
            override fun onDataChannel(channel: DataChannel) {}
            override fun onRenegotiationNeeded() {}
            override fun onTrack(transceiver: RtpTransceiver) {}
        })

        localStream?.let {
            peerConnection?.addStream(it)
        }
    }

    private fun createOffer() {
        val constraints = MediaConstraints()
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        roomRef.set(hashMapOf("offer" to desc.description, "type" to desc.type.canonicalForm()))
                    }

                    override fun onSetFailure(p0: String?) {}
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
//                    override fun onSetFailure(p0: Exception?) {}
                }, desc)
            }

            override fun onCreateFailure(msg: String?) {}
            override fun onSetSuccess() {}
            override fun onSetFailure(p0: String?) {}
        }, constraints)
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

                    override fun onSetFailure(p0: String?) {}
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
//                    override fun onSetFailure(p0: Exception?) {}
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
                        roomRef.update("answer", desc.description)
                        roomRef.update("type", desc.type.canonicalForm())
                    }

                    override fun onSetFailure(p0: String?) {}
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
//                    override fun onSetFailure(p0: Exception?) {}
                }, desc)
            }

            override fun onCreateFailure(msg: String?) {}
            override fun onSetSuccess() {}
            override fun onSetFailure(p0: String?) {}
        }, constraints)
    }

    private fun listenForIceCandidates() {
        roomRef.collection("candidates").addSnapshotListener { snapshot, _ ->
            snapshot?.documentChanges?.forEach { change ->
                val data = change.document.data
                val candidate = IceCandidate(
                    data["sdpMid"] as String?,
                    (data["sdpMLineIndex"] as Long).toInt(),
                    data["candidate"] as String
                )
                peerConnection?.addIceCandidate(candidate)
            }
        }
    }

    fun endCall() {
        peerConnection?.close()
        peerConnection = null
        roomRef.delete()
    }
}
