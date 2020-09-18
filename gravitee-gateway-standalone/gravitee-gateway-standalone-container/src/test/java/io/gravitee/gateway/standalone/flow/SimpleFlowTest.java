/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.standalone.flow;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.standalone.AbstractWiremockGatewayTest;
import io.gravitee.gateway.standalone.flow.policy.MyPolicy;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.gateway.standalone.policy.KeylessPolicy;
import io.gravitee.gateway.standalone.policy.PolicyBuilder;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyPlugin;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/flow/simple-flow.json")
public class SimpleFlowTest extends AbstractWiremockGatewayTest {

    private final Map<Class<?>, Object> policies = new HashMap<>();

    @Before
    public void setUp() {
        policies.clear();

        policies.put(KeylessPolicy.class, Mockito.mock(KeylessPolicy.class));
    }

    @Test
    public void shouldRunFlows_getMethod() throws Exception {
        wireMockRule.stubFor(get("/team/my_team").willReturn(ok()));

        MyPolicy policy = Mockito.mock(MyPolicy.class);
        policies.put(MyPolicy.class, policy);

        final HttpResponse response = Request.Get("http://localhost:8082/test/my_team").execute().returnResponse();
        assertEquals(HttpStatusCode.OK_200, response.getStatusLine().getStatusCode());
        wireMockRule.verify(getRequestedFor(urlPathEqualTo("/team/my_team")).withHeader("my-counter", equalTo("1")));

        Mockito.verify(policy, Mockito.times(2)).onRequest(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void shouldNotRunFlows_deleteMethod() throws Exception {
        wireMockRule.stubFor(delete("/team/my_team").willReturn(ok()));

        final HttpResponse response = Request.Delete("http://localhost:8082/test/my_team").execute().returnResponse();
        assertEquals(HttpStatusCode.OK_200, response.getStatusLine().getStatusCode());
        wireMockRule.verify(deleteRequestedFor(urlPathEqualTo("/team/my_team")).withoutHeader("my-counter"));
    }

    @Override
    public void register(ConfigurablePluginManager<PolicyPlugin> policyPluginManager) {
        super.register(policyPluginManager);

        PolicyPlugin myPolicy = PolicyBuilder.build("my-policy", MyPolicy.class);
        policyPluginManager.register(myPolicy);
    }

    /*
    @Override
    public Object create(PolicyMetadata policyMetadata, PolicyConfiguration policyConfiguration) {
        return policies.get(policyMetadata.policy());
    }
     */
}