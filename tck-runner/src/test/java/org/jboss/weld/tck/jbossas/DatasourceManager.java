/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.weld.tck.jbossas;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.dmr.ModelNode;
import org.testng.annotations.BeforeSuite;

import java.io.IOException;

/**
 * Sanity saving measure that forces the TCK to fail early if java:/DefaultDS does not exist.
 * Otherwise the TCK will get 90% of the way through and then hang.
 *
 *
 * @author Stuart Douglas
 */
public class DatasourceManager {

    @BeforeSuite
    public void beforeSuite() throws IOException {

        ModelControllerClient client = ModelControllerClient.Factory.create("localhost", 9999);
        ModelNode request = new ModelNode();
        request.get("operation").set("read-resource");
        request.get("address").get("subsystem").set("datasources");
        // request.get("address").set("subsystem", "threads");
        request.get("recursive").set(false);
        ModelNode r = client.execute(OperationBuilder.Factory.create(request).build());
        boolean found = false;
        for(ModelNode dataSource : r.get("result").get("data-source").asList()) {
            if(dataSource.asProperty().getName().equals("java:/DefaultDS")) {
                found = true;
            }
        }
        if(!found) {
            throw new RuntimeException("Datasource java:/DefaultDS was not found. This datasource must be defined, or the TCK will hang half way through due to missing MSC dependencies");
        }
    }

}
