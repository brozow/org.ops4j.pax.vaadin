package org.ops4j.pax.vaadin.samples.internal.client.mycomponent;

import com.vaadin.shared.communication.ClientRpc;

public interface MyComponentClientRpc extends ClientRpc {

    // TODO example API
    public void alert(String message);

}
