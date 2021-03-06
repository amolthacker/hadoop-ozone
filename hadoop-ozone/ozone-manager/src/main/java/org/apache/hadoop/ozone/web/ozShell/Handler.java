/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.hadoop.ozone.web.ozShell;

import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.Callable;

import org.apache.hadoop.hdds.cli.GenericParentCommand;
import org.apache.hadoop.hdds.cli.HddsVersionProvider;
import org.apache.hadoop.hdds.conf.OzoneConfiguration;
import org.apache.hadoop.hdds.server.JsonUtils;

import org.apache.hadoop.ozone.OzoneSecurityUtil;
import org.apache.hadoop.ozone.client.OzoneClient;
import org.apache.hadoop.ozone.client.OzoneClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

/**
 * Base class for shell commands that connect via Ozone client.
 */
@Command(mixinStandardHelpOptions = true,
    versionProvider = HddsVersionProvider.class)
@SuppressWarnings("squid:S106") // CLI
public abstract class Handler implements Callable<Void> {

  protected static final Logger LOG = LoggerFactory.getLogger(Handler.class);

  private OzoneConfiguration conf;

  @ParentCommand
  private GenericParentCommand parent;

  public boolean isVerbose() {
    return parent.isVerbose();
  }

  public OzoneConfiguration createOzoneConfiguration() {
    return parent.createOzoneConfiguration();
  }

  protected abstract OzoneAddress getAddress() throws OzoneClientException;

  protected abstract void execute(OzoneClient client, OzoneAddress address)
      throws IOException, OzoneClientException;

  @Override
  public Void call() throws Exception {
    conf = createOzoneConfiguration();

    OzoneAddress address = getAddress();
    try (OzoneClient client = createClient(address)) {
      if (isVerbose()) {
        address.print(out());
      }
      execute(client, address);
    }

    return null;
  }

  protected OzoneClient createClient(OzoneAddress address)
      throws IOException, OzoneClientException {
    return address.createClient(conf);
  }

  protected boolean securityEnabled(String commandName) {
    boolean enabled = OzoneSecurityUtil.isSecurityEnabled(conf);
    if (!enabled) {
      err().printf("Error: '%s' operation works only when security is " +
          "enabled. To enable security set ozone.security.enabled to " +
          "true.%n", commandName);
    }
    return enabled;
  }

  protected void printObjectAsJson(Object o) throws IOException {
    out().println(JsonUtils.toJsonStringWithDefaultPrettyPrinter(o));
  }

  protected OzoneConfiguration getConf() {
    return conf;
  }

  protected PrintStream out() {
    return System.out;
  }

  protected PrintStream err() {
    return System.err;
  }

}
