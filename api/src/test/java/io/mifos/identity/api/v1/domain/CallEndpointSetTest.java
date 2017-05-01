/*
 * Copyright 2017 The Mifos Initiative.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mifos.identity.api.v1.domain;

import io.mifos.core.test.domain.ValidationTest;
import io.mifos.core.test.domain.ValidationTestCase;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Myrle Krantz
 */
@RunWith(Parameterized.class)
public class CallEndpointSetTest extends ValidationTest<CallEndpointSet> {

  public CallEndpointSetTest(final ValidationTestCase<CallEndpointSet> testCase) {
    super(testCase);
  }

  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<ValidationTestCase> ret = new ArrayList<>();

    ret.add(new ValidationTestCase<CallEndpointSet>("validCase")
            .adjustment(x -> {})
            .valid(true));
    ret.add(new ValidationTestCase<CallEndpointSet>("invalid identifier")
            .adjustment(x -> x.setIdentifier(null))
            .valid(false));
    ret.add(new ValidationTestCase<CallEndpointSet>("null list")
            .adjustment(x -> x.setPermittableEndpointGroupIdentifiers(null))
            .valid(false));

    return ret;
  }

  @Override
  protected CallEndpointSet createValidTestSubject() {
    final CallEndpointSet ret = new CallEndpointSet();
    ret.setIdentifier("blah");
    ret.setPermittableEndpointGroupIdentifiers(Collections.emptyList());
    return ret;
  }

}