package com.example.callerscreen.WebRTC

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

    fun joinCall(roomId: String, isCaller: Boolean) {
        WebRTCManager.roomId = roomId
        roomRef = firestore.collection("calls").document(roomId)
        createPeerConnection()

//        if (isCaller) {
//            createOffer()
//        } else {
            listenForOffer()
//        }

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

//
//import android.annotation.SuppressLint
//import android.content.Context
//import android.util.Log
//import org.webrtc.*
//import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.firestore.ListenerRegistration
//
//object WebRTCManager {
//
//    private lateinit var peerConnectionFactory: PeerConnectionFactory
//    private lateinit var eglBase: EglBase
//
//    lateinit var localRenderer: SurfaceViewRenderer
//    lateinit var remoteRenderer: SurfaceViewRenderer
//    private lateinit var localVideoTrack: VideoTrack
//    private lateinit var videoCapturer: VideoCapturer
//    private lateinit var localStream: MediaStream
//    private var peerConnection: PeerConnection? = null
//
//    @SuppressLint("StaticFieldLeak")
//    private val signalingRef = FirebaseFirestore.getInstance()
//    private var signalingListener: ListenerRegistration? = null
//
//    private const val TAG = "WebRTCManager"
//
//    fun init(context: Context) {
//        val initializationOptions = PeerConnectionFactory.InitializationOptions.builder(context)
//            .setEnableInternalTracer(true)
//            .createInitializationOptions()
//
//        PeerConnectionFactory.initialize(initializationOptions)
//
//        eglBase = EglBase.create()
//
//        val options = PeerConnectionFactory.Options()
//        val encoderFactory = DefaultVideoEncoderFactory(
//            eglBase.eglBaseContext, true, true
//        )
//        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)
//
//        peerConnectionFactory = PeerConnectionFactory.builder()
//            .setOptions(options)
//            .setVideoEncoderFactory(encoderFactory)
//            .setVideoDecoderFactory(decoderFactory)
//            .createPeerConnectionFactory()
//    }
//
//    fun initSurfaceViews(context: Context) {
//        localRenderer = SurfaceViewRenderer(context)
//        remoteRenderer = SurfaceViewRenderer(context)
//
//        localRenderer.init(eglBase.eglBaseContext, null)
//        remoteRenderer.init(eglBase.eglBaseContext, null)
//
//        localRenderer.setZOrderMediaOverlay(true)
//        remoteRenderer.setZOrderMediaOverlay(false)
//    }
//
//    fun startLocalMedia(context: Context) {
//        videoCapturer = createVideoCapturer(context)
//
//        val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
//        val videoSource = peerConnectionFactory.createVideoSource(false)
//        videoCapturer.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)
//
//        localVideoTrack = peerConnectionFactory.createVideoTrack("LOCAL_VIDEO_TRACK", videoSource)
//        localVideoTrack.addSink(localRenderer)
//
//        videoCapturer.startCapture(640, 480, 30)
//
//        localStream = peerConnectionFactory.createLocalMediaStream("LOCAL_STREAM")
//        localStream.addTrack(localVideoTrack)
//    }
//
//    fun createPeerConnection(context: Context) {
//        val rtcConfig = PeerConnection.RTCConfiguration(emptyList())
//
//        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
//            override fun onIceCandidate(candidate: IceCandidate?) {
//                if (candidate != null) {
//                    signalingRef.collection("calls").document("room123")
//                        .collection("candidates").add(candidate.toMap())
//                }
//            }
//
//            override fun onAddStream(stream: MediaStream?) {
//                stream?.videoTracks?.firstOrNull()?.addSink(remoteRenderer)
//            }
//
//            // Empty overrides
//            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
//            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {}
//            override fun onIceConnectionReceivingChange(p0: Boolean) {}
//            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
//            override fun onRemoveStream(p0: MediaStream?) {}
//            override fun onDataChannel(p0: DataChannel?) {}
//            override fun onRenegotiationNeeded() {}
//            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
//            override fun onTrack(transceiver: RtpTransceiver?) {}
//            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {}
//        })
//
//        peerConnection?.addStream(localStream)
//    }
//
//    fun listenForSignalingMessages() {
//        signalingListener = signalingRef.collection("calls").document("room123")
//            .addSnapshotListener { snapshot, _ ->
//                val data = snapshot?.data ?: return@addSnapshotListener
//                val status = data["status"] as? String
//                Log.d(TAG, "Received signaling status: $status")
//                // TODO: Implement offer/answer signaling based on status
//            }
//    }
//
//    fun closeConnection() {
//        videoCapturer.stopCapture()
//        videoCapturer.dispose()
//        localRenderer.release()
//        remoteRenderer.release()
//        peerConnection?.close()
//        peerConnection = null
//        signalingListener?.remove()
//    }
//
//    private fun createVideoCapturer(context: Context): VideoCapturer {
//        val cameraEnumerator = Camera2Enumerator(context)
//        val deviceNames = cameraEnumerator.deviceNames
//        for (deviceName in deviceNames) {
//            if (cameraEnumerator.isFrontFacing(deviceName)) {
//                return cameraEnumerator.createCapturer(deviceName, null)
//            }
//        }
//        throw IllegalStateException("No front-facing camera found.")
//    }
//
//    private fun IceCandidate.toMap(): Map<String, Any> = mapOf(
//        "sdpMid" to sdpMid.orEmpty(),
//        "sdpMLineIndex" to sdpMLineIndex,
//        "candidate" to sdp
//    )
//
//    fun createOffer(roomId: String) {
//        peerConnection?.createOffer(object : SdpObserver {
//            override fun onCreateSuccess(sessionDescription: SessionDescription) {
//                peerConnection?.setLocalDescription(this, sessionDescription)
//                val offer = hashMapOf(
//                    "type" to sessionDescription.type.canonicalForm(),
//                    "sdp" to sessionDescription.description
//                )
//                signalingRef.collection("calls").document(roomId)
//                    .set(mapOf("offer" to offer))
//                    .addOnSuccessListener {
//                        Log.d(TAG, "Offer saved to Firestore.")
//                    }
//                    .addOnFailureListener { e ->
//                        Log.e(TAG, "Failed to save offer: $e")
//                    }
//            }
//
//            override fun onSetSuccess() {}
//            override fun onCreateFailure(error: String) {
//                Log.e(TAG, "Offer creation failed: $error")
//            }
//            override fun onSetFailure(error: String) {}
//        }, MediaConstraints())
//    }
//
//    fun listenForAnswer(roomId: String) {
//        signalingRef.collection("calls").document(roomId)
//            .addSnapshotListener { snapshot, _ ->
//                val data = snapshot?.data ?: return@addSnapshotListener
//                val answer = data["answer"] as? Map<*, *> ?: return@addSnapshotListener
//                val sdp = answer["sdp"] as? String ?: return@addSnapshotListener
//                val sessionDescription = SessionDescription(SessionDescription.Type.ANSWER, sdp)
//                peerConnection?.setRemoteDescription(object : SdpObserver {
//                    override fun onSetSuccess() {
//                        Log.d(TAG, "Remote description set.")
//                    }
//
//                    override fun onSetFailure(error: String) {
//                        Log.e(TAG, "Failed to set remote description: $error")
//                    }
//
//                    override fun onCreateSuccess(sessionDescription: SessionDescription) {}
//                    override fun onCreateFailure(error: String) {}
//                }, sessionDescription)
//            }
//    }
//
//
//    fun createAnswer(roomId: String) {
//        peerConnection?.createAnswer(object : SdpObserver {
//            override fun onCreateSuccess(sessionDescription: SessionDescription) {
//                peerConnection?.setLocalDescription(this, sessionDescription)
//                val answer = hashMapOf(
//                    "type" to sessionDescription.type.canonicalForm(),
//                    "sdp" to sessionDescription.description
//                )
//                signalingRef.collection("calls").document(roomId)
//                    .update("answer", answer)
//                    .addOnSuccessListener {
//                        Log.d(TAG, "Answer saved to Firestore.")
//                    }
//                    .addOnFailureListener { e ->
//                        Log.e(TAG, "Failed to save answer: $e")
//                    }
//            }
//
//            override fun onSetSuccess() {}
//            override fun onCreateFailure(error: String) {
//                Log.e(TAG, "Answer creation failed: $error")
//            }
//            override fun onSetFailure(error: String) {}
//        }, MediaConstraints())
//    }
//
//
//    fun listenForOffer(roomId: String) {
//        signalingRef.collection("calls").document(roomId)
//            .addSnapshotListener { snapshot, _ ->
//                val data = snapshot?.data ?: return@addSnapshotListener
//                val offer = data["offer"] as? Map<*, *> ?: return@addSnapshotListener
//                val sdp = offer["sdp"] as? String ?: return@addSnapshotListener
//                val sessionDescription = SessionDescription(SessionDescription.Type.OFFER, sdp)
//                peerConnection?.setRemoteDescription(object : SdpObserver {
//                    override fun onSetSuccess() {
//                        Log.d(TAG, "Remote description set.")
//                        createAnswer(roomId)
//                    }
//
//                    override fun onSetFailure(error: String) {
//                        Log.e(TAG, "Failed to set remote description: $error")
//                    }
//
//                    override fun onCreateSuccess(sessionDescription: SessionDescription) {}
//                    override fun onCreateFailure(error: String) {}
//                }, sessionDescription)
//            }
//    }
//
//    fun sendIceCandidate(roomId: String, candidate: IceCandidate) {
//        val candidateData = hashMapOf(
//            "sdpMid" to candidate.sdpMid,
//            "sdpMLineIndex" to candidate.sdpMLineIndex,
//            "candidate" to candidate.sdp
//        )
//        signalingRef.collection("calls").document(roomId)
//            .collection("callerCandidates")
//            .add(candidateData)
//    }
//
//    fun listenForIceCandidates(roomId: String, collection: String) {
//        signalingRef.collection("calls").document(roomId)
//            .collection(collection)
//            .addSnapshotListener { snapshots, _ ->
//                for (document in snapshots?.documentChanges ?: return@addSnapshotListener) {
//                    val data = document.document.data
//                    val candidate = IceCandidate(
//                        data["sdpMid"] as String,
//                        (data["sdpMLineIndex"] as Long).toInt(),
//                        data["candidate"] as String
//                    )
//                    peerConnection?.addIceCandidate(candidate)
//                }
//            }
//    }
//
//}
