/*
 * Licensed to the Sakai Foundation (SF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership. The SF licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.sakaiproject.kernel.persondirectory.providers;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPAttributeSet;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchConstraints;
import com.novell.ldap.LDAPSearchResults;

import org.easymock.classextension.EasyMock;
import org.junit.Test;
import org.sakaiproject.kernel.api.ldap.LdapConnectionBroker;
import org.sakaiproject.kernel.api.ldap.LdapException;
import org.sakaiproject.kernel.api.persondirectory.Person;
import org.sakaiproject.kernel.api.persondirectory.PersonProviderException;

import java.util.Iterator;
import java.util.Set;

public class LdapPersonProviderTest {
  /**
   * Test for the default constructor. Too simple to not have and boosts code
   * coverage.
   *
   * @throws Exception
   */
  @Test
  public void testDefaultConstructor() throws Exception {
    try {
      LdapPersonProvider provider = new LdapPersonProvider();
      provider.getPerson("whatever", null);
      fail("Should fail when the broker isn't explicitly set or injected by OSGi reference.");
    } catch (NullPointerException e) {
      // expected
    }
  }

  /**
   * Test getting a person from an ldap provider.
   * 
   * @throws Exception
   */
  @Test
  public void testGetPerson() throws Exception {
    LdapPersonProvider provider = setUpForPositiveTest();
    Person person = provider.getPerson("tUser", null);
    assertNotNull(person);

    Set<String> attributeNames = person.getAttributeNames();
    assertNotNull(attributeNames);

    assertEquals(2, attributeNames.size());

    assertTrue(attributeNames.contains("firstname"));
    assertEquals("Tester", person.getAttributeValue("firstname"));

    assertTrue(attributeNames.contains("lastname"));
    assertEquals("User", person.getAttributeValue("lastname"));
  }

  /**
   * Test getPerson() when LdapConnectionBroker.getBoundConnection(..) throws an
   * LdapException.
   *
   * @throws Exception
   */
  @Test
  public void testGetPersonThrowsLdapException() throws Exception {
    LdapConnectionBroker broker = createMock(LdapConnectionBroker.class);
    expect(
        broker.getBoundConnection((String) anyObject(), (String) anyObject(), (String) anyObject()))
        .andThrow(new LdapException("oops"));
    replay(broker);
    LdapPersonProvider provider = new LdapPersonProvider(broker);
    try {
      provider.getPerson("tUser", null);
      fail("Should bubble up exceptions that are thrown internally.");
    } catch (PersonProviderException e) {
      // expected
    }
  }

  /**
   * Test getPerson() when LDAPConnection.search(..) throws an LDAPException.
   *
   * @throws Exception
   */
  @Test
  public void testGetPersonThrowsLDAPException() throws Exception {
    LdapConnectionBroker broker = createMock(LdapConnectionBroker.class);
    LDAPConnection connection = EasyMock.createMock(LDAPConnection.class);
    expect(broker.getBoundConnection(isA(String.class), (String) anyObject(), (String) anyObject()))
        .andReturn(connection);
    replay(broker);
    expect(
        connection.search(isA(String.class), anyInt(), isA(String.class), (String[]) anyObject(),
            anyBoolean(), isA(LDAPSearchConstraints.class))).andThrow(new LDAPException());
    EasyMock.replay(connection);

    LdapPersonProvider provider = new LdapPersonProvider(broker);
    try {
      provider.getPerson("tUser", null);
      fail("Should bubble up exceptions that are thrown internally.");
    } catch (PersonProviderException e) {
      // expected
    }
  }

  /**
   * Setup everything needed for a test that follows the most positive path of
   * action.
   *
   * @return
   * @throws Exception
   */
  private LdapPersonProvider setUpForPositiveTest() throws Exception {
    LDAPConnection connection = EasyMock.createMock(LDAPConnection.class);
    LDAPSearchResults results = EasyMock.createMock(LDAPSearchResults.class);
    LDAPAttributeSet attrSet = EasyMock.createMock(LDAPAttributeSet.class);
    Iterator attrIter = createMock(Iterator.class);
    LDAPEntry entry = EasyMock.createMock(LDAPEntry.class);

    LdapConnectionBroker broker = createMock(LdapConnectionBroker.class);
    expect(broker.getBoundConnection(isA(String.class), (String) anyObject(), (String) anyObject()))
        .andReturn(connection);
    replay(broker);
    expect(
        connection.search(isA(String.class), anyInt(), isA(String.class), (String[]) anyObject(),
            anyBoolean(), isA(LDAPSearchConstraints.class))).andReturn(results);
    EasyMock.replay(connection);

    // get a result
    expect(results.hasMore()).andReturn(TRUE);
    expect(results.next()).andReturn(entry);

    // get the attributes and an iterator to them
    expect(entry.getAttributeSet()).andReturn(attrSet);
    expect(attrSet.iterator()).andReturn(attrIter);
    EasyMock.replay(entry, attrSet);

    // first loop through
    expect(attrIter.hasNext()).andReturn(TRUE);
    LDAPAttribute attr = EasyMock.createMock(LDAPAttribute.class);
    expect(attr.getName()).andReturn("firstname");
    expect(attr.getStringValueArray()).andReturn(new String[] { "Tester" });
    expect(attrIter.next()).andReturn(attr);
    EasyMock.replay(attr);

    // second loop through
    expect(attrIter.hasNext()).andReturn(TRUE);
    attr = EasyMock.createMock(LDAPAttribute.class);
    expect(attr.getName()).andReturn("lastname");
    expect(attr.getStringValueArray()).andReturn(new String[] { "User" });
    expect(attrIter.next()).andReturn(attr);
    EasyMock.replay(attr);

    // stop loop through attributes
    expect(attrIter.hasNext()).andReturn(FALSE);
    replay(attrIter);

    // stop loop through results
    expect(results.hasMore()).andReturn(FALSE);
    EasyMock.replay(results);

    return new LdapPersonProvider(broker);
  }
}
