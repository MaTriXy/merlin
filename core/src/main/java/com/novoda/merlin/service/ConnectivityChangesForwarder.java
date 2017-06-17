package com.novoda.merlin.service;

import com.novoda.merlin.NetworkStatus;
import com.novoda.merlin.receiver.ConnectivityChangeEvent;
import com.novoda.merlin.registerable.bind.BindCallbackManager;
import com.novoda.merlin.registerable.connection.ConnectListener;
import com.novoda.merlin.registerable.disconnection.DisconnectListener;

class ConnectivityChangesForwarder {

    private final NetworkStatusRetriever networkStatusRetriever;
    private final DisconnectListener disconnectListener;
    private final ConnectListener connectListener;
    private final BindCallbackManager bindCallbackManager;
    private final EndpointPinger endpointPinger;

    private NetworkStatus lastEndpointPingNetworkStatus;

    ConnectivityChangesForwarder(NetworkStatusRetriever networkStatusRetriever,
                                 DisconnectListener disconnectListener,
                                 ConnectListener connectListener,
                                 BindCallbackManager bindCallbackManager,
                                 EndpointPinger endpointPinger) {
        this.networkStatusRetriever = networkStatusRetriever;
        this.disconnectListener = disconnectListener;
        this.connectListener = connectListener;
        this.bindCallbackManager = bindCallbackManager;
        this.endpointPinger = endpointPinger;
    }

    void forwardInitialNetworkStatus() {
        if (hasPerformedEndpointPing()) {
            bindCallbackManager.onMerlinBind(lastEndpointPingNetworkStatus);
            return;
        }

        bindCallbackManager.onMerlinBind(networkStatusRetriever.retrieveNetworkStatus());
    }

    private boolean hasPerformedEndpointPing() {
        return lastEndpointPingNetworkStatus != null;
    }

    void forward(ConnectivityChangeEvent connectivityChangeEvent) {
        if (matchesPreviousEndpointPingNetworkStatus(connectivityChangeEvent)) {
            return;
        }

        networkStatusRetriever.fetchWithPing(endpointPinger, endpointPingerCallback);
        lastEndpointPingNetworkStatus = connectivityChangeEvent.asNetworkStatus();
    }

    private boolean matchesPreviousEndpointPingNetworkStatus(ConnectivityChangeEvent connectivityChangeEvent) {
        return connectivityChangeEvent.asNetworkStatus().equals(lastEndpointPingNetworkStatus);
    }

    private final EndpointPinger.PingerCallback endpointPingerCallback = new EndpointPinger.PingerCallback() {
        @Override
        public void onSuccess() {
            lastEndpointPingNetworkStatus = NetworkStatus.newAvailableInstance();
            if (connectListener != null) {
                connectListener.onConnect();
            }
        }

        @Override
        public void onFailure() {
            lastEndpointPingNetworkStatus = NetworkStatus.newUnavailableInstance();
            if (disconnectListener != null) {
                disconnectListener.onDisconnect();
            }
        }
    };

}
