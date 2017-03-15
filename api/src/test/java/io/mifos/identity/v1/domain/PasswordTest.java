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
package io.mifos.identity.v1.domain;

import io.mifos.core.test.domain.ValidationTestCase;
import io.mifos.identity.api.v1.domain.Password;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Myrle Krantz
 */
@RunWith(Parameterized.class)
public class PasswordTest {
  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<ValidationTestCase> ret = new ArrayList<>();

    ret.add(new ValidationTestCase<Password>("validCase")
            .adjustment(x -> {})
            .valid(true));
    ret.add(new ValidationTestCase<Password>("spacesInPassword")
            .adjustment(x -> x.setPassword("         "))
            .valid(false));
    ret.add(new ValidationTestCase<Password>("tooShortPassword")
            .adjustment(x -> x.setPassword("1234567"))
            .valid(false));

    return ret;
  }

  private final ValidationTestCase<Password> testCase;

  public PasswordTest(final ValidationTestCase<Password> testCase)
  {
    this.testCase = testCase;
  }

  private Password createValidTestSubject()
  {
    return new Password("golden_osiris");
  }

  @Test()
  public void test(){
    final Password testSubject = createValidTestSubject();
    testCase.getAdjustment().accept(testSubject);
    Assert.assertTrue(testCase.toString(), testCase.check(testSubject));
  }
}