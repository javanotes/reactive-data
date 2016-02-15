/* ============================================================================
*
* FILE: ModelJdbcRepository.java
*
The MIT License (MIT)

Copyright (c) 2016 Sutanu Dalui

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*
* ============================================================================
*/
package com.reactivetechnologies.analytics.store;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.repository.CrudRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.util.Assert;

import com.reactivetechnologies.analytics.core.RegressionModel;

/**
 * A very basic jdbc store for saving the ensembled output.
 * No connection pooling. No performance optimization. No transaction.
 */
public class WekaModelJdbcRepository
    implements CrudRepository<RegressionModel, Long> {

  private static final Logger log = LoggerFactory.getLogger(WekaModelJdbcRepository.class);
  @Autowired
  private JdbcTemplate jdbcTemplate;
  
  static final String CREATE_TABLE = "create table if not exists RD_MODEL_SNAPSHOT ("
      + "CREATED_TS TIMESTAMP,"
      + "MODEL MEDIUMTEXT,"
      + "GEN_ID BIGINT(20) PRIMARY KEY"
      + ")";
  
  static final String INSERT_MODEL = "insert into RD_MODEL_SNAPSHOT (CREATED_TS,MODEL,GEN_ID) values (now(),?,?) ";
  static final String EXISTS_MODEL = "select count(*) from RD_MODEL_SNAPSHOT where GEN_ID = ";
  static final String COUNT_MODELS = "select count(*) from RD_MODEL_SNAPSHOT ";
  static final String DELETE_MODEL = "delete from RD_MODEL_SNAPSHOT where GEN_ID = ";
  static final String SELECT_MODEL = "select MODEL, GEN_ID, CREATED_TS from RD_MODEL_SNAPSHOT where GEN_ID = ";
  
  @PostConstruct
  void init()
  {
    jdbcTemplate.execute(CREATE_TABLE);
    log.info("Init scripts ran..");
  }
  @Override
  public <S extends RegressionModel> S save(S entity) {
    Assert.notNull(entity);
    log.debug("Firing insert: "+INSERT_MODEL);
    try {
      jdbcTemplate.update(INSERT_MODEL, entity.serializeClassifierAsJson(), entity.getLongId());
    }
    catch(DuplicateKeyException dup)
    {
      log.warn("Model already exists in database. Ignoring save failure");
    }
    catch (Exception e) {
      throw new DataAccessException("Cannot serialize ensemble classifier to disk", e) {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;
      };
    }
    return entity;
  }

  @Override
  public <S extends RegressionModel> Iterable<S> save(Iterable<S> entities) {
    Assert.notNull(entities);
    List<S> models = new ArrayList<>();
    for(S m : entities)
    {
      models.add(save(m));
    }
    return models;
  }

  @Override
  public RegressionModel findOne(Long id) {
    final List<RegressionModel> models = new ArrayList<>();
    jdbcTemplate.query(SELECT_MODEL + id, new RowCallbackHandler() {
      
      @Override
      public void processRow(ResultSet rs) throws SQLException {
        RegressionModel m = new RegressionModel();
        try {
          m.deserializeClassifierFromJson(rs.getString(1));
          m.setLongId(rs.getLong(2));
          m.setGeneratedOn(rs.getDate(3));
          models.add(m);
        } catch (IOException e) {
          throw new SQLException("Unable to deserialze model from string", e);
        }
        
      }
    });
    return models.isEmpty() ? null : models.get(0);
  }

  @Override
  public boolean exists(Long id) {
    Long count = jdbcTemplate.queryForObject(EXISTS_MODEL + id, Long.class);
    return count > 0;
  }

  @Override
  public Iterable<RegressionModel> findAll() {
    // NOOP
    return null;
  }

  @Override
  public Iterable<RegressionModel> findAll(Iterable<Long> ids) {
    Assert.notNull(ids);
    List<RegressionModel> list = new ArrayList<>();
    for(Long id : ids)
    {
      list.add(findOne(id));
    }
    return list;
  }

  @Override
  public long count() {
    return jdbcTemplate.queryForObject(COUNT_MODELS, Long.class);
  }

  @Override
  public void delete(Long id) {
    jdbcTemplate.update(DELETE_MODEL + id);
  }

  @Override
  public void delete(RegressionModel entity) {
    delete(entity.getLongId());
  }

  @Override
  public void delete(Iterable<? extends RegressionModel> entities) {
    Assert.notNull(entities);
    for(RegressionModel m : entities)
    {
      delete(m);
    }
  }

  @Override
  public void deleteAll() {
    // NOOP

  }

}
