package com.example.drinktalk.util

import android.util.Log
import com.google.android.gms.nearby.connection.*

/**
 * Custom singleton class to handle nearby connections API usage in the project
 */
object ConnectionSingleton {

    private lateinit var mConnectionsClient: ConnectionsClient
    private val mRemoteEndpointIds: MutableList<String> = arrayListOf()
    private val mRemoteEndpointNicks: MutableMap<String, String> = mutableMapOf()
    private lateinit var mListener: Listener

    private var connectionHash: String = ""
    private var nickname: String = ""
    private var host: Boolean = false

    /**
     * Hosts a server with given connection hash
     */
    fun startAdvertising(){
        mConnectionsClient.startAdvertising(
            nickname,
            connectionHash,
            mConnectionLifecycleCallback,
            AdvertisingOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        )
            .addOnSuccessListener {
                debug("Successfully started advertising")
            }
            .addOnFailureListener {
                debug("Failed to start advertising: $it")
            }
    }

    fun stopAdvertising(){
        mConnectionsClient.stopAdvertising()
        debug("Successfully stopped advertising")
    }

    /**
     * Starts a search for host with given connection hash
     */
    fun startDiscovery(){
        mConnectionsClient.startDiscovery(
            connectionHash,
            mEndpointDiscoveryCallback,
            DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        )
            .addOnSuccessListener {
                debug("Successfully started discovery")
            }
            .addOnFailureListener {
                debug("Failed to start discovery: $it")
            }

    }

    fun stopDiscovery(){
        mConnectionsClient.stopDiscovery()
        debug("Successfully stopped discovery")
    }

    private val mConnectionLifecycleCallback = object : ConnectionLifecycleCallback() {

        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            debug("Connection initiated on $endpointId:${connectionInfo.endpointName}")

            // Automatically accept the connection on both sides.
            mConnectionsClient.acceptConnection(endpointId, mPayloadCallback)

            mRemoteEndpointNicks[endpointId] = connectionInfo.endpointName
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            debug("Connection result status on $endpointId = " + result.status.toString())

            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    debug("Connected on $endpointId")
                    // We're connected! Can now start sending and receiving data.
                    // Add the endpoint to the list
                    mRemoteEndpointIds.add(endpointId)

                    mListener.onConnected(endpointId, mRemoteEndpointNicks[endpointId].toString());
                }

                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    // The connection was rejected by one or both sides.
                    debug("Connection rejected from $endpointId")
                }

                ConnectionsStatusCodes.STATUS_ERROR -> {
                    // The connection broke before it was able to be accepted.
                    debug("Connection error on accepting from $endpointId")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            // We've been disconnected from this endpoint. No more data can be
            // sent or received.
            debug("Disconnected from $endpointId")
            mListener.onDisconnected(endpointId, mRemoteEndpointNicks[endpointId].toString())
            disconnectEndpoint(endpointId)
        }

    }

    private val mEndpointDiscoveryCallback = object : EndpointDiscoveryCallback() {

        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            // An endpoint was found. Request connection to it.
            debug("Found endpoint $endpointId:$info:${info.endpointName}")

            mConnectionsClient.requestConnection(
                nickname,
                endpointId,
                mConnectionLifecycleCallback
            )
                .addOnSuccessListener {
                    // Connection successfully requested
                    // Both parties need to accept the request
                    debug("Successfully requested connection.")
                }
                .addOnFailureListener {
                    debug("Failed to connect: $it")
                }
        }

        override fun onEndpointLost(endpointId: String) {
            // A previously discovered endpoint has gone away.
            debug("Endpoint: $endpointId was lost")
        }
    }

    private val mPayloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            debug("mPayloadCallback.onPayloadReceived $endpointId")

            // Only byte type payload is needed / supported
            when (payload.type) {
                Payload.Type.BYTES -> {
                    val data = payload.asBytes()!!

                    // Make a call to our custom listener
                    mListener.onResponse(endpointId, data.toString(Charsets.UTF_8));

                    debug("Payload.Type.BYTES: ${data.toString(Charsets.UTF_8)}")
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            debug("Payload transferred to $endpointId")
        }
    }

    /**
     * Send message to a single endpoint
     *
     * @param receiver endpointId of a receiving endpoint
     * @param message
     */
    fun sendMessage(receiver: String, message: String){
        mConnectionsClient.sendPayload(
            receiver,
            Payload.fromBytes(message.toByteArray(Charsets.UTF_8))
        )
    }

    /**
     * Disconnects from an endpoint and removes it from the connected endpoint list
     *
     * @param endpointId
     */
    private fun disconnectEndpoint(endpointId: String){
        mConnectionsClient.disconnectFromEndpoint(endpointId)
        mRemoteEndpointIds.remove(endpointId)
        mRemoteEndpointNicks.remove(endpointId)
    }

    /**
     * Broadcasts message to all connected endpoints
     *
     * @param message
     */
    fun sendToAll(message: String){
        mRemoteEndpointIds.forEach {
            mConnectionsClient.sendPayload(
                it,
                Payload.fromBytes(message.toByteArray(Charsets.UTF_8))
            )
        }
    }

    /**
     * Fully close the nearby connection client, disconnects from all endpoints
     */
    fun close(){
        mConnectionsClient.stopAdvertising()
        mConnectionsClient.stopDiscovery()
        mConnectionsClient.stopAllEndpoints()
    }

    fun setConnectionClient(connectionsClient: ConnectionsClient){
        this.mConnectionsClient = connectionsClient
    }

    fun getRemoteEndPoints(): List<String>{
        return mRemoteEndpointIds
    }

    fun getConnectionHash(): String{
        return connectionHash
    }

    fun setConnectionHash(hash: String){
        this.connectionHash = hash
    }

    fun getNickname(): String{
        return nickname
    }

    fun setNickname(nick: String){
        this.nickname = nick
    }

    fun getHost(): Boolean{
        return host
    }

    fun setHost(hostBool: Boolean){
        this.host = hostBool
    }

    fun addListener(listener: Listener){
        mListener = listener
    }

    /**
     * Custom logging method
     *
     * @param message a message to be logged in Logcat
     */
    private fun debug(message: String){
        Log.d("NearbyConnection", message)
    }

    /**
     * Custom listener interface to hook onto connection events
     */
    interface Listener {

        /**
         * Listener for received messages from endpoint
         *
         * @param endpointId
         * @param message the message received from an endpoint
         */
        fun onResponse(endpointId: String, message: String)

        /**
         * Listener for established connection with endpoint
         *
         * @param endpointId
         * @param nickname connected endpoint nickname
         */
        fun onConnected(endpointId: String, nickname: String)

        /**
         * Listener for getting disconnected from endpoint
         *
         * @param endpointId
         */
        fun onDisconnected(endpointId: String, nickname: String)
    }
}