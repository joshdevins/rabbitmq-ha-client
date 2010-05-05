/*
 * Copyright 2010 Josh Devins
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.joshdevins.rabbitmq.client.ha;

import com.rabbitmq.client.ShutdownSignalException;

/**
 * Abstract implementation of {@link HaConnectionListener} with empty method implementations.
 * 
 * @author Josh Devins
 */
public abstract class AbstractHaConnectionListener implements HaConnectionListener {

    public void onConnectFailure(final HaConnectionProxy connectionProxy, final Exception exception) {
    }

    public void onConnection(final HaConnectionProxy connectionProxy) {
    }

    public void onDisconnect(final HaConnectionProxy connectionProxy,
            final ShutdownSignalException shutdownSignalException) {
    }

    public void onReconnectFailure(final HaConnectionProxy connectionProxy, final Exception exception) {
    }

    public void onReconnection(final HaConnectionProxy connectionProxy) {
    }
}
