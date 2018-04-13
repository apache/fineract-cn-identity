/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.cn.identity.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.fineract.cn.anubis.config.EnableAnubis;
import org.apache.fineract.cn.async.config.EnableAsync;
import org.apache.fineract.cn.cassandra.config.EnableCassandra;
import org.apache.fineract.cn.command.config.EnableCommandProcessing;
import org.apache.fineract.cn.crypto.config.EnableCrypto;
import org.apache.fineract.cn.identity.internal.util.IdentityConstants;
import org.apache.fineract.cn.lang.config.EnableServiceException;
import org.apache.fineract.cn.lang.config.EnableTenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@EnableAutoConfiguration
@EnableWebMvc
@EnableDiscoveryClient
@EnableAsync
@EnableTenantContext
@EnableCassandra
@EnableCommandProcessing
@EnableServiceException
@EnableCrypto
@EnableAnubis(provideSignatureStorage = false)
@ComponentScan({
    "org.apache.fineract.cn.identity.rest",
    "org.apache.fineract.cn.identity.internal.service",
    "org.apache.fineract.cn.identity.internal.repository",
    "org.apache.fineract.cn.identity.internal.command.handler"
})
public class IdentityServiceConfig extends WebMvcConfigurerAdapter {

  public IdentityServiceConfig() {}


  @Bean(name = IdentityConstants.JSON_SERIALIZER_NAME)
  public Gson gson() {
    return new GsonBuilder().create();
  }

  @Bean(name = IdentityConstants.LOGGER_NAME)
  public Logger logger() {
    return LoggerFactory.getLogger(IdentityConstants.LOGGER_NAME);
  }

  @Override
  public void configurePathMatch(final PathMatchConfigurer configurer) {
    configurer.setUseSuffixPatternMatch(Boolean.FALSE);
  }

  public static void main(String[] args) {
    SpringApplication.run(IdentityServiceConfig.class, args);
  }
}
