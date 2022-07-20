/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.quarkus.component.cxf.soap.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(CxfSoapClientTestResource.class)
class CxfSoapClientTest {

    @Test
    public void loadComponentCxf() {
        /* A simple autogenerated test */
        RestAssured.get("/cxf-soap/load/component/cxf")
                .then()
                .statusCode(200);
    }

    @Test
    public void testSimpleSoapClient() {
        final String response = RestAssured.get("/cxf-soap/sendSimpleSoapRequest")
                .then()
                .statusCode(201)
                .assertThat()
                .extract().asString();
        assertEquals("Hello CamelQuarkusCXF", response);
    }

    @Test
    public void testWSSecurity() {
        final String response = RestAssured.get("/cxf-soap/wsSecurity")
                .then()
                .statusCode(201)
                .assertThat()
                .extract().asString();
        assertEquals("Hello WSSecurity CamelQuarkusCXF", response);
    }

    @Test
    public void testComplexSoapClient() {
        final String response = RestAssured.get("/cxf-soap/sendComplexSoapRequest")
                .then()
                .statusCode(201)
                .assertThat()
                .extract().asString();
        assertTrue(response.contains("greeting=Hello"));
    }
}