/**
 * Copyright 2015 Flipkart Internet Pvt. Ltd.
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

package com.flipkart.ranger.finder;

import com.flipkart.ranger.model.Deserializer;
import com.flipkart.ranger.model.ServiceNodeSelector;
import com.flipkart.ranger.model.ServiceRegistry;
import com.flipkart.ranger.model.ShardSelector;
import com.google.common.base.Preconditions;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseServiceFinderBuilder<T, RegistryType extends ServiceRegistry<T>, FinderType extends ServiceFinder<T, RegistryType>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseServiceFinderBuilder.class);

    private String namespace;
    private String serviceName;
    private CuratorFramework curatorFramework;
    private String connectionString;
    private int nodeRefreshIntervalMs;
    private boolean disableWatchers;
    private Deserializer<T> deserializer;
    private ShardSelector<T, RegistryType> shardSelector;
    private ServiceNodeSelector<T> nodeSelector = new RandomServiceNodeSelector<>();

    public BaseServiceFinderBuilder<T, RegistryType, FinderType> withNamespace(final String namespace) {
        this.namespace = namespace;
        return this;
    }

    public BaseServiceFinderBuilder<T, RegistryType, FinderType> withServiceName(final String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    public BaseServiceFinderBuilder<T, RegistryType, FinderType> withCuratorFramework(CuratorFramework curatorFramework) {
        this.curatorFramework = curatorFramework;
        return this;
    }

    public BaseServiceFinderBuilder<T, RegistryType, FinderType> withConnectionString(final String connectionString) {
        this.connectionString = connectionString;
        return this;
    }

    public BaseServiceFinderBuilder<T, RegistryType, FinderType> withDeserializer(Deserializer<T> deserializer) {
        this.deserializer = deserializer;
        return this;
    }

    public BaseServiceFinderBuilder<T, RegistryType, FinderType> withShardSelector(ShardSelector<T, RegistryType> shardSelector) {
        this.shardSelector = shardSelector;
        return this;
    }

    public BaseServiceFinderBuilder<T, RegistryType, FinderType> withNodeSelector(ServiceNodeSelector<T> nodeSelector) {
        this.nodeSelector = nodeSelector;
        return this;
    }

    public BaseServiceFinderBuilder<T, RegistryType, FinderType> withNodeRefreshIntervalMs(int nodeRefreshIntervalMs) {
        this.nodeRefreshIntervalMs = nodeRefreshIntervalMs;
        return this;
    }

    public BaseServiceFinderBuilder<T, RegistryType, FinderType> withDisableWatchers() {
        this.disableWatchers = true;
        return this;
    }

    public BaseServiceFinderBuilder<T, RegistryType, FinderType> withDisableWatchers(boolean disableWatchers) {
        this.disableWatchers = disableWatchers;
        return this;
    }

    public FinderType build() {
        Preconditions.checkNotNull(namespace);
        Preconditions.checkNotNull(serviceName);
        Preconditions.checkNotNull(deserializer);
        if( null == curatorFramework) {
            Preconditions.checkNotNull(connectionString);
            curatorFramework = CuratorFrameworkFactory.builder()
                    .namespace(namespace)
                    .connectString(connectionString)
                    .retryPolicy(new ExponentialBackoffRetry(1000, 100)).build();
            curatorFramework.start();
        }
        if(nodeRefreshIntervalMs < 1000) {
            LOGGER.warn("Node refresh interval for {} is too low: {} ms. Has been upgraded to 1000ms ",
                    serviceName, nodeRefreshIntervalMs);
            nodeRefreshIntervalMs = 1000;
        }
        Service service = new Service(curatorFramework, namespace, serviceName);
        return buildFinder(service, deserializer, shardSelector, nodeSelector, nodeRefreshIntervalMs, disableWatchers);
    }

    protected abstract FinderType buildFinder(Service service,
                                              Deserializer<T> deserializer,
                                              ShardSelector<T, RegistryType> shardSelector,
                                              ServiceNodeSelector<T> nodeSelector,
                                              int healthcheckRefreshTimeMillis,
                                              boolean disableWatchers);

}
