/* ============================================================================
*
* FILE: HzKeyValueAdapterTest.java
*
* MODULE DESCRIPTION:
* See class description
*
* Copyright (C) 2015 
*
* All rights reserved
*
* ============================================================================
*/
package com.reactivetechnologies.springdata.hz;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import java.io.Serializable;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.data.util.CloseableIterator;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ObjectUtils;

import com.reactivetechnologies.platform.Configurator;
import com.reactivetechnologies.platform.datagrid.HazelcastKeyValueAdapterBean;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Configurator.class)
public class HzKeyValueAdapterTest {
  private static final String COLLECTION_1 = "collection-1";
  private static final String COLLECTION_2 = "collection-2";
  private static final String STRING_1 = new String("1");

  private Object object1 = new SimpleObject("one");
  private Object object2 = new SimpleObject("two");

  @Autowired
  private HazelcastKeyValueAdapterBean adapter;
  
  
  @Before
  public void setUp() {
    
    adapter.deleteAllOf(COLLECTION_1);
    adapter.deleteAllOf(COLLECTION_2);
    adapter.deleteAllOf(STRING_1);
    adapter.deleteAllOf(COLLECTION_1);
    
  }
  @After
  public void tearDown() {
    
  }

  /**
   * @see DATACMNS-525
   */
  @Test(expected = IllegalArgumentException.class)
  public void putShouldThrowExceptionWhenAddingNullId() {
    adapter.put(null, object1, COLLECTION_1);
  }

  /**
   * @see DATACMNS-525
   */
  @Test(expected = IllegalArgumentException.class)
  public void putShouldThrowExceptionWhenCollectionIsNullValue() {
    adapter.put("1", object1, null);
  }

  /**
   * @see DATACMNS-525
   */
  @Test
  public void putReturnsNullWhenNoObjectForIdPresent() {
    adapter.delete("1", COLLECTION_1);
    assertThat(adapter.put("1", object1, COLLECTION_1), nullValue());
  }

  /**
   * @see DATACMNS-525
   */
  @Test
  public void putShouldReturnPreviousObjectForIdWhenAddingNewOneWithSameIdPresent() {

    adapter.put("1", object1, COLLECTION_1);
    assertThat(adapter.put("1", object2, COLLECTION_1), equalTo(object1));
  }

  /**
   * @see DATACMNS-525
   */
  @Test(expected = IllegalArgumentException.class)
  public void containsShouldThrowExceptionWhenIdIsNull() {
    adapter.contains(null, COLLECTION_1);
  }

  /**
   * @see DATACMNS-525
   */
  @Test(expected = IllegalArgumentException.class)
  public void containsShouldThrowExceptionWhenTypeIsNull() {
    adapter.contains("", null);
  }

  /**
   * @see DATACMNS-525
   */
  @Test
  public void containsShouldReturnFalseWhenNoElementsPresent() {
    adapter.delete("1", COLLECTION_1);
    assertThat(adapter.contains("1", COLLECTION_1), is(false));
  }

  /**
   * @see DATACMNS-525
   */
  @Test
  public void containShouldReturnTrueWhenElementWithIdPresent() {

    adapter.put("1", object1, COLLECTION_1);
    assertThat(adapter.contains("1", COLLECTION_1), is(true));
  }

  /**
   * @see DATACMNS-525
   */
  @Test
  public void getShouldReturnNullWhenNoElementWithIdPresent() {
    adapter.delete("1", COLLECTION_1);
    assertThat(adapter.get("1", COLLECTION_1), nullValue());
  }

  /**
   * @see DATACMNS-525
   */
  @Test
  public void getShouldReturnElementWhenMatchingIdPresent() {

    adapter.put("1", object1, COLLECTION_1);
    assertThat(adapter.get("1", COLLECTION_1), is(object1));
  }

  /**
   * @see DATACMNS-525
   */
  @Test(expected = IllegalArgumentException.class)
  public void getShouldThrowExceptionWhenIdIsNull() {
    adapter.get(null, COLLECTION_1);
  }

  /**
   * @see DATACMNS-525
   */
  @Test(expected = IllegalArgumentException.class)
  public void getShouldThrowExceptionWhenTypeIsNull() {
    adapter.get("1", null);
  }

  /**
   * @see DATACMNS-525
   */
  @Test
  public void getAllOfShouldReturnAllValuesOfGivenCollection() {

    adapter.put("1", object1, COLLECTION_1);
    adapter.put("2", object2, COLLECTION_1);
    adapter.put("3", STRING_1, COLLECTION_2);

    //assertThat(adapter.getAllOf(COLLECTION_1), containsInAnyOrder(object1, object2));
  }

  /**
   * @see DATACMNS-525
   */
  @Test(expected = IllegalArgumentException.class)
  public void getAllOfShouldThrowExceptionWhenTypeIsNull() {
    adapter.getAllOf(null);
  }

  /**
   * @see DATACMNS-525
   */
  @Test
  public void deleteShouldReturnNullWhenGivenIdThatDoesNotExist() {
    adapter.delete("1", COLLECTION_1);
    assertThat(adapter.delete("1", COLLECTION_1), nullValue());
  }

  /**
   * @see DATACMNS-525
   */
  @Test
  public void deleteShouldReturnDeletedObject() {

    adapter.put("1", object1, COLLECTION_1);
    assertThat(adapter.delete("1", COLLECTION_1), is(object1));
  }

  /**
   * @see DATAKV-99
   */
  //@Test
  public void scanShouldIterateOverAvailableEntries() {

    adapter.put("1", object1, COLLECTION_1);
    adapter.put("2", object2, COLLECTION_1);

    CloseableIterator<Map.Entry<Serializable, Object>> iterator = adapter.entries(COLLECTION_1);

    //assertThat(iterator.next(), isEntry("1", object1));
    //assertThat(iterator.next(), isEntry("2", object2));
    assertThat(iterator.hasNext(), is(false));
  }

  /**
   * @see DATAKV-99
   */
  //@Test
  public void scanShouldReturnEmptyIteratorWhenNoElementsAvailable() {
    assertThat(adapter.entries(COLLECTION_1).hasNext(), is(false));
  }

  /**
   * @see DATAKV-99
   */
  //@Test
  public void scanDoesNotMixResultsFromMultipleKeyspaces() {

    adapter.put("1", object1, COLLECTION_1);
    adapter.put("2", object2, COLLECTION_2);

    CloseableIterator<Map.Entry<Serializable, Object>> iterator = adapter.entries(COLLECTION_1);

    //assertThat(iterator.next(), isEntry("1", object1));
    assertThat(iterator.hasNext(), is(false));
  }

  static class SimpleObject implements Serializable{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    protected String stringValue;

    public SimpleObject() {}

    SimpleObject(String value) {
      this.stringValue = value;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * ObjectUtils.nullSafeHashCode(this.stringValue);
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (!(obj instanceof SimpleObject)) {
        return false;
      }
      SimpleObject that = (SimpleObject) obj;
      return ObjectUtils.nullSafeEquals(this.stringValue, that.stringValue);
    }
  }
}
