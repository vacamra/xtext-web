/**
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.xtext.web.server.test;

import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Singleton;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtend.lib.annotations.AccessorType;
import org.eclipse.xtend.lib.annotations.Accessors;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;
import org.eclipse.xtext.validation.ResourceValidatorImpl;
import org.eclipse.xtext.web.example.statemachine.StatemachineRuntimeModule;
import org.eclipse.xtext.web.server.IServiceResult;
import org.eclipse.xtext.web.server.ServiceConflictResult;
import org.eclipse.xtext.web.server.XtextServiceDispatcher;
import org.eclipse.xtext.web.server.model.DocumentStateResult;
import org.eclipse.xtext.web.server.model.XtextWebDocument;
import org.eclipse.xtext.web.server.persistence.ResourceContentResult;
import org.eclipse.xtext.web.server.test.AbstractWebServerTest;
import org.eclipse.xtext.web.server.test.HashMapSession;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.Pair;
import org.eclipse.xtext.xbase.lib.Pure;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("all")
public class UpdateDocumentTest extends AbstractWebServerTest {
  /**
   * The resource validator is applied asynchronously after each update.
   */
  @Singleton
  @Accessors(AccessorType.PUBLIC_GETTER)
  public static class TestResourceValidator extends ResourceValidatorImpl {
    private Thread workerThread;
    
    private long sleepTime;
    
    private volatile boolean canceled;
    
    private int entryCounter;
    
    private int exitCounter;
    
    @Override
    public List<Issue> validate(final Resource resource, final CheckMode mode, final CancelIndicator mon) {
      try {
        List<Issue> _xblockexpression = null;
        {
          this.workerThread = Thread.currentThread();
          synchronized (this) {
            this.entryCounter++;
            this.notifyAll();
          }
          final long startTime = System.currentTimeMillis();
          while (((((System.currentTimeMillis() - startTime) < this.sleepTime) && (!mon.isCanceled())) && (!this.workerThread.isInterrupted()))) {
            Thread.sleep(30);
          }
          boolean _isCanceled = mon.isCanceled();
          if (_isCanceled) {
            this.canceled = true;
          }
          synchronized (this) {
            this.exitCounter++;
            this.notifyAll();
          }
          _xblockexpression = super.validate(resource, mode, mon);
        }
        return _xblockexpression;
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    }
    
    public int reset(final long sleepTime) {
      int _xblockexpression = (int) 0;
      {
        if ((this.workerThread != null)) {
          this.workerThread.interrupt();
        }
        this.workerThread = null;
        this.sleepTime = sleepTime;
        this.canceled = false;
        this.entryCounter = 0;
        _xblockexpression = this.exitCounter = 0;
      }
      return _xblockexpression;
    }
    
    public synchronized void waitUntil(final Function1<? super UpdateDocumentTest.TestResourceValidator, ? extends Boolean> condition) {
      try {
        final long startTime = System.currentTimeMillis();
        while ((!(condition.apply(this)).booleanValue())) {
          {
            long _currentTimeMillis = System.currentTimeMillis();
            long _minus = (_currentTimeMillis - startTime);
            boolean _lessThan = (_minus < 8000);
            Assert.assertTrue(_lessThan);
            this.wait(3000);
          }
        }
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    }
    
    @Pure
    public Thread getWorkerThread() {
      return this.workerThread;
    }
    
    @Pure
    public long getSleepTime() {
      return this.sleepTime;
    }
    
    @Pure
    public boolean isCanceled() {
      return this.canceled;
    }
    
    @Pure
    public int getEntryCounter() {
      return this.entryCounter;
    }
    
    @Pure
    public int getExitCounter() {
      return this.exitCounter;
    }
  }
  
  @Inject
  private UpdateDocumentTest.TestResourceValidator resourceValidator;
  
  @Inject
  private ExecutorService executorService;
  
  @Override
  protected Module getRuntimeModule() {
    abstract class __UpdateDocumentTest_1 extends StatemachineRuntimeModule {
      public abstract Class<? extends IResourceValidator> bindIResourceValidator();
    }
    
    return new __UpdateDocumentTest_1() {
      public Class<? extends IResourceValidator> bindIResourceValidator() {
        return UpdateDocumentTest.TestResourceValidator.class;
      }
    };
  }
  
  @Test
  public void testCorrectStateId() {
    this.resourceValidator.reset(0);
    final File file = this.createFile("input signal x state foo end");
    final HashMapSession session = new HashMapSession();
    Pair<String, String> _mappedTo = Pair.<String, String>of("serviceType", "update");
    String _name = file.getName();
    Pair<String, String> _mappedTo_1 = Pair.<String, String>of("resource", _name);
    Pair<String, String> _mappedTo_2 = Pair.<String, String>of("deltaText", "bar");
    Pair<String, String> _mappedTo_3 = Pair.<String, String>of("deltaOffset", "21");
    Pair<String, String> _mappedTo_4 = Pair.<String, String>of("deltaReplaceLength", "3");
    XtextServiceDispatcher.ServiceDescriptor update = this.getService(
      Collections.<String, String>unmodifiableMap(CollectionLiterals.<String, String>newHashMap(_mappedTo, _mappedTo_1, _mappedTo_2, _mappedTo_3, _mappedTo_4)), session);
    Assert.assertTrue(update.isHasSideEffects());
    IServiceResult _apply = update.getService().apply();
    final DocumentStateResult updateResult = ((DocumentStateResult) _apply);
    Pair<String, String> _mappedTo_5 = Pair.<String, String>of("serviceType", "update");
    String _name_1 = file.getName();
    Pair<String, String> _mappedTo_6 = Pair.<String, String>of("resource", _name_1);
    Pair<String, String> _mappedTo_7 = Pair.<String, String>of("deltaText", " set x = true");
    Pair<String, String> _mappedTo_8 = Pair.<String, String>of("deltaOffset", "24");
    Pair<String, String> _mappedTo_9 = Pair.<String, String>of("deltaReplaceLength", "0");
    String _stateId = updateResult.getStateId();
    Pair<String, String> _mappedTo_10 = Pair.<String, String>of("requiredStateId", _stateId);
    update = this.getService(
      Collections.<String, String>unmodifiableMap(CollectionLiterals.<String, String>newHashMap(_mappedTo_5, _mappedTo_6, _mappedTo_7, _mappedTo_8, _mappedTo_9, _mappedTo_10)), session);
    update.getService().apply();
    Pair<String, String> _mappedTo_11 = Pair.<String, String>of("serviceType", "load");
    String _name_2 = file.getName();
    Pair<String, String> _mappedTo_12 = Pair.<String, String>of("resource", _name_2);
    final XtextServiceDispatcher.ServiceDescriptor load = this.getService(Collections.<String, String>unmodifiableMap(CollectionLiterals.<String, String>newHashMap(_mappedTo_11, _mappedTo_12)), session);
    IServiceResult _apply_1 = load.getService().apply();
    final ResourceContentResult loadResult = ((ResourceContentResult) _apply_1);
    Assert.assertEquals("input signal x state bar set x = true end", loadResult.getFullText());
  }
  
  @Test
  public void testIncorrectStateId1() {
    this.resourceValidator.reset(0);
    final File file = this.createFile("state foo end");
    Pair<String, String> _mappedTo = Pair.<String, String>of("serviceType", "update");
    String _name = file.getName();
    Pair<String, String> _mappedTo_1 = Pair.<String, String>of("resource", _name);
    Pair<String, String> _mappedTo_2 = Pair.<String, String>of("deltaText", " set x = true");
    Pair<String, String> _mappedTo_3 = Pair.<String, String>of("deltaOffset", "10");
    Pair<String, String> _mappedTo_4 = Pair.<String, String>of("deltaReplaceLength", "0");
    Pair<String, String> _mappedTo_5 = Pair.<String, String>of("requiredStateId", "totalerquatsch");
    final XtextServiceDispatcher.ServiceDescriptor update = this.getService(
      Collections.<String, String>unmodifiableMap(CollectionLiterals.<String, String>newHashMap(_mappedTo, _mappedTo_1, _mappedTo_2, _mappedTo_3, _mappedTo_4, _mappedTo_5)));
    Assert.assertTrue(update.isHasConflict());
    final IServiceResult result = update.getService().apply();
    Assert.<IServiceResult>assertThat(result, IsInstanceOf.<IServiceResult>instanceOf(ServiceConflictResult.class));
    Assert.assertEquals(((ServiceConflictResult) result).getConflict(), "invalidStateId");
  }
  
  @Test
  public void testIncorrectStateId2() {
    this.resourceValidator.reset(0);
    final File file = this.createFile("input signal x state foo end");
    final HashMapSession session = new HashMapSession();
    Pair<String, String> _mappedTo = Pair.<String, String>of("serviceType", "update");
    String _name = file.getName();
    Pair<String, String> _mappedTo_1 = Pair.<String, String>of("resource", _name);
    Pair<String, String> _mappedTo_2 = Pair.<String, String>of("deltaText", "bar");
    Pair<String, String> _mappedTo_3 = Pair.<String, String>of("deltaOffset", "21");
    Pair<String, String> _mappedTo_4 = Pair.<String, String>of("deltaReplaceLength", "3");
    final XtextServiceDispatcher.ServiceDescriptor update1 = this.getService(
      Collections.<String, String>unmodifiableMap(CollectionLiterals.<String, String>newHashMap(_mappedTo, _mappedTo_1, _mappedTo_2, _mappedTo_3, _mappedTo_4)), session);
    IServiceResult _apply = update1.getService().apply();
    final DocumentStateResult updateResult = ((DocumentStateResult) _apply);
    Pair<String, String> _mappedTo_5 = Pair.<String, String>of("serviceType", "update");
    String _name_1 = file.getName();
    Pair<String, String> _mappedTo_6 = Pair.<String, String>of("resource", _name_1);
    Pair<String, String> _mappedTo_7 = Pair.<String, String>of("deltaText", " set x = true");
    Pair<String, String> _mappedTo_8 = Pair.<String, String>of("deltaOffset", "24");
    Pair<String, String> _mappedTo_9 = Pair.<String, String>of("deltaReplaceLength", "0");
    String _stateId = updateResult.getStateId();
    Pair<String, String> _mappedTo_10 = Pair.<String, String>of("requiredStateId", _stateId);
    final XtextServiceDispatcher.ServiceDescriptor update2 = this.getService(
      Collections.<String, String>unmodifiableMap(CollectionLiterals.<String, String>newHashMap(_mappedTo_5, _mappedTo_6, _mappedTo_7, _mappedTo_8, _mappedTo_9, _mappedTo_10)), session);
    Pair<String, String> _mappedTo_11 = Pair.<String, String>of("serviceType", "update");
    String _name_2 = file.getName();
    Pair<String, String> _mappedTo_12 = Pair.<String, String>of("resource", _name_2);
    Pair<String, String> _mappedTo_13 = Pair.<String, String>of("deltaText", "y");
    Pair<String, String> _mappedTo_14 = Pair.<String, String>of("deltaOffset", "12");
    Pair<String, String> _mappedTo_15 = Pair.<String, String>of("deltaReplaceLength", "1");
    String _stateId_1 = updateResult.getStateId();
    Pair<String, String> _mappedTo_16 = Pair.<String, String>of("requiredStateId", _stateId_1);
    final XtextServiceDispatcher.ServiceDescriptor update3 = this.getService(
      Collections.<String, String>unmodifiableMap(CollectionLiterals.<String, String>newHashMap(_mappedTo_11, _mappedTo_12, _mappedTo_13, _mappedTo_14, _mappedTo_15, _mappedTo_16)), session);
    update2.getService().apply();
    final IServiceResult result = update3.getService().apply();
    Assert.<IServiceResult>assertThat(result, IsInstanceOf.<IServiceResult>instanceOf(ServiceConflictResult.class));
    Assert.assertEquals(((ServiceConflictResult) result).getConflict(), "invalidStateId");
  }
  
  @Test
  public void testNoBackgroundWorkWhenConflict() {
    this.resourceValidator.reset(0);
    final File file = this.createFile("input signal x state foo end");
    final HashMapSession session = new HashMapSession();
    Pair<String, String> _mappedTo = Pair.<String, String>of("serviceType", "update");
    String _name = file.getName();
    Pair<String, String> _mappedTo_1 = Pair.<String, String>of("resource", _name);
    Pair<String, String> _mappedTo_2 = Pair.<String, String>of("deltaText", "bar");
    Pair<String, String> _mappedTo_3 = Pair.<String, String>of("deltaOffset", "21");
    Pair<String, String> _mappedTo_4 = Pair.<String, String>of("deltaReplaceLength", "3");
    XtextServiceDispatcher.ServiceDescriptor update = this.getService(
      Collections.<String, String>unmodifiableMap(CollectionLiterals.<String, String>newHashMap(_mappedTo, _mappedTo_1, _mappedTo_2, _mappedTo_3, _mappedTo_4)), session);
    Assert.assertTrue(update.isHasSideEffects());
    IServiceResult _apply = update.getService().apply();
    final DocumentStateResult updateResult = ((DocumentStateResult) _apply);
    Pair<String, String> _mappedTo_5 = Pair.<String, String>of("serviceType", "update");
    String _name_1 = file.getName();
    Pair<String, String> _mappedTo_6 = Pair.<String, String>of("resource", _name_1);
    Pair<String, String> _mappedTo_7 = Pair.<String, String>of("deltaText", " set x = true");
    Pair<String, String> _mappedTo_8 = Pair.<String, String>of("deltaOffset", "24");
    Pair<String, String> _mappedTo_9 = Pair.<String, String>of("deltaReplaceLength", "0");
    String _stateId = updateResult.getStateId();
    Pair<String, String> _mappedTo_10 = Pair.<String, String>of("requiredStateId", _stateId);
    update = this.getService(
      Collections.<String, String>unmodifiableMap(CollectionLiterals.<String, String>newHashMap(_mappedTo_5, _mappedTo_6, _mappedTo_7, _mappedTo_8, _mappedTo_9, _mappedTo_10)), session);
    String _name_2 = file.getName();
    Pair<Class<XtextWebDocument>, String> _mappedTo_11 = Pair.<Class<XtextWebDocument>, String>of(XtextWebDocument.class, _name_2);
    final XtextWebDocument document = session.<XtextWebDocument>get(_mappedTo_11);
    XtextResource _resource = document.getResource();
    _resource.setModificationStamp(1234);
    final IServiceResult result = update.getService().apply();
    Assert.<IServiceResult>assertThat(result, IsInstanceOf.<IServiceResult>instanceOf(ServiceConflictResult.class));
    Pair<String, String> _mappedTo_12 = Pair.<String, String>of("serviceType", "load");
    String _name_3 = file.getName();
    Pair<String, String> _mappedTo_13 = Pair.<String, String>of("resource", _name_3);
    final XtextServiceDispatcher.ServiceDescriptor load = this.getService(Collections.<String, String>unmodifiableMap(CollectionLiterals.<String, String>newHashMap(_mappedTo_12, _mappedTo_13)), session);
    IServiceResult _apply_1 = load.getService().apply();
    final ResourceContentResult loadResult = ((ResourceContentResult) _apply_1);
    Assert.assertEquals("input signal x state bar end", loadResult.getFullText());
  }
  
  @Test
  public void testCancelBackgroundWork1() {
    this.resourceValidator.reset(300);
    final File file = this.createFile("input signal x state foo end");
    final HashMapSession session = new HashMapSession();
    Pair<String, String> _mappedTo = Pair.<String, String>of("serviceType", "update");
    String _name = file.getName();
    Pair<String, String> _mappedTo_1 = Pair.<String, String>of("resource", _name);
    Pair<String, String> _mappedTo_2 = Pair.<String, String>of("deltaText", "bar");
    Pair<String, String> _mappedTo_3 = Pair.<String, String>of("deltaOffset", "21");
    Pair<String, String> _mappedTo_4 = Pair.<String, String>of("deltaReplaceLength", "3");
    final XtextServiceDispatcher.ServiceDescriptor update1 = this.getService(
      Collections.<String, String>unmodifiableMap(CollectionLiterals.<String, String>newHashMap(_mappedTo, _mappedTo_1, _mappedTo_2, _mappedTo_3, _mappedTo_4)), session);
    IServiceResult _apply = update1.getService().apply();
    final DocumentStateResult updateResult = ((DocumentStateResult) _apply);
    Pair<String, String> _mappedTo_5 = Pair.<String, String>of("serviceType", "update");
    String _name_1 = file.getName();
    Pair<String, String> _mappedTo_6 = Pair.<String, String>of("resource", _name_1);
    Pair<String, String> _mappedTo_7 = Pair.<String, String>of("deltaText", " set x = true");
    Pair<String, String> _mappedTo_8 = Pair.<String, String>of("deltaOffset", "24");
    Pair<String, String> _mappedTo_9 = Pair.<String, String>of("deltaReplaceLength", "0");
    String _stateId = updateResult.getStateId();
    Pair<String, String> _mappedTo_10 = Pair.<String, String>of("requiredStateId", _stateId);
    final XtextServiceDispatcher.ServiceDescriptor update2 = this.getService(
      Collections.<String, String>unmodifiableMap(CollectionLiterals.<String, String>newHashMap(_mappedTo_5, _mappedTo_6, _mappedTo_7, _mappedTo_8, _mappedTo_9, _mappedTo_10)), session);
    final Function1<UpdateDocumentTest.TestResourceValidator, Boolean> _function = (UpdateDocumentTest.TestResourceValidator it) -> {
      return Boolean.valueOf((it.entryCounter == 1));
    };
    this.resourceValidator.waitUntil(_function);
    final Callable<IServiceResult> _function_1 = () -> {
      return update2.getService().apply();
    };
    this.executorService.<IServiceResult>submit(_function_1);
    final Function1<UpdateDocumentTest.TestResourceValidator, Boolean> _function_2 = (UpdateDocumentTest.TestResourceValidator it) -> {
      return Boolean.valueOf((it.exitCounter == 1));
    };
    this.resourceValidator.waitUntil(_function_2);
    Assert.assertTrue(this.resourceValidator.canceled);
    final Function1<UpdateDocumentTest.TestResourceValidator, Boolean> _function_3 = (UpdateDocumentTest.TestResourceValidator it) -> {
      return Boolean.valueOf((it.entryCounter == 2));
    };
    this.resourceValidator.waitUntil(_function_3);
  }
  
  @Test
  public void testCancelBackgroundWork2() {
    this.resourceValidator.reset(300);
    final File file = this.createFile("input signal x state foo end");
    final HashMapSession session = new HashMapSession();
    Pair<String, String> _mappedTo = Pair.<String, String>of("serviceType", "update");
    String _name = file.getName();
    Pair<String, String> _mappedTo_1 = Pair.<String, String>of("resource", _name);
    Pair<String, String> _mappedTo_2 = Pair.<String, String>of("deltaText", "bar");
    Pair<String, String> _mappedTo_3 = Pair.<String, String>of("deltaOffset", "21");
    Pair<String, String> _mappedTo_4 = Pair.<String, String>of("deltaReplaceLength", "3");
    final XtextServiceDispatcher.ServiceDescriptor update = this.getService(
      Collections.<String, String>unmodifiableMap(CollectionLiterals.<String, String>newHashMap(_mappedTo, _mappedTo_1, _mappedTo_2, _mappedTo_3, _mappedTo_4)), session);
    IServiceResult _apply = update.getService().apply();
    final DocumentStateResult updateResult = ((DocumentStateResult) _apply);
    Pair<String, String> _mappedTo_5 = Pair.<String, String>of("serviceType", "assist");
    String _name_1 = file.getName();
    Pair<String, String> _mappedTo_6 = Pair.<String, String>of("resource", _name_1);
    Pair<String, String> _mappedTo_7 = Pair.<String, String>of("caretOffset", "15");
    String _stateId = updateResult.getStateId();
    Pair<String, String> _mappedTo_8 = Pair.<String, String>of("requiredStateId", _stateId);
    final XtextServiceDispatcher.ServiceDescriptor contentAssist = this.getService(
      Collections.<String, String>unmodifiableMap(CollectionLiterals.<String, String>newHashMap(_mappedTo_5, _mappedTo_6, _mappedTo_7, _mappedTo_8)), session);
    final Function1<UpdateDocumentTest.TestResourceValidator, Boolean> _function = (UpdateDocumentTest.TestResourceValidator it) -> {
      return Boolean.valueOf((it.entryCounter == 1));
    };
    this.resourceValidator.waitUntil(_function);
    final Callable<IServiceResult> _function_1 = () -> {
      return contentAssist.getService().apply();
    };
    this.executorService.<IServiceResult>submit(_function_1);
    final Function1<UpdateDocumentTest.TestResourceValidator, Boolean> _function_2 = (UpdateDocumentTest.TestResourceValidator it) -> {
      return Boolean.valueOf((it.exitCounter == 1));
    };
    this.resourceValidator.waitUntil(_function_2);
    Assert.assertTrue(this.resourceValidator.canceled);
    final Function1<UpdateDocumentTest.TestResourceValidator, Boolean> _function_3 = (UpdateDocumentTest.TestResourceValidator it) -> {
      return Boolean.valueOf((it.entryCounter == 2));
    };
    this.resourceValidator.waitUntil(_function_3);
  }
  
  @Test
  public void testCancelLowPriorityService1() {
    this.resourceValidator.reset(3000);
    final File file = this.createFile("state foo end");
    final HashMapSession session = new HashMapSession();
    Pair<String, String> _mappedTo = Pair.<String, String>of("serviceType", "validate");
    String _name = file.getName();
    Pair<String, String> _mappedTo_1 = Pair.<String, String>of("resource", _name);
    final XtextServiceDispatcher.ServiceDescriptor validate = this.getService(Collections.<String, String>unmodifiableMap(CollectionLiterals.<String, String>newHashMap(_mappedTo, _mappedTo_1)), session);
    Pair<String, String> _mappedTo_2 = Pair.<String, String>of("serviceType", "update");
    String _name_1 = file.getName();
    Pair<String, String> _mappedTo_3 = Pair.<String, String>of("resource", _name_1);
    Pair<String, String> _mappedTo_4 = Pair.<String, String>of("deltaText", "bar");
    Pair<String, String> _mappedTo_5 = Pair.<String, String>of("deltaOffset", "6");
    Pair<String, String> _mappedTo_6 = Pair.<String, String>of("deltaReplaceLength", "3");
    final XtextServiceDispatcher.ServiceDescriptor update = this.getService(
      Collections.<String, String>unmodifiableMap(CollectionLiterals.<String, String>newHashMap(_mappedTo_2, _mappedTo_3, _mappedTo_4, _mappedTo_5, _mappedTo_6)), session);
    final Callable<IServiceResult> _function = () -> {
      return validate.getService().apply();
    };
    this.executorService.<IServiceResult>submit(_function);
    final Function1<UpdateDocumentTest.TestResourceValidator, Boolean> _function_1 = (UpdateDocumentTest.TestResourceValidator it) -> {
      return Boolean.valueOf((it.entryCounter == 1));
    };
    this.resourceValidator.waitUntil(_function_1);
    update.getService().apply();
    final Function1<UpdateDocumentTest.TestResourceValidator, Boolean> _function_2 = (UpdateDocumentTest.TestResourceValidator it) -> {
      return Boolean.valueOf((it.exitCounter == 1));
    };
    this.resourceValidator.waitUntil(_function_2);
    Assert.assertTrue(this.resourceValidator.canceled);
    final Function1<UpdateDocumentTest.TestResourceValidator, Boolean> _function_3 = (UpdateDocumentTest.TestResourceValidator it) -> {
      return Boolean.valueOf((it.entryCounter == 2));
    };
    this.resourceValidator.waitUntil(_function_3);
  }
  
  @Test
  public void testCancelLowPriorityService2() {
    this.resourceValidator.reset(3000);
    final File file = this.createFile("state foo end");
    final HashMapSession session = new HashMapSession();
    Pair<String, String> _mappedTo = Pair.<String, String>of("serviceType", "validate");
    String _name = file.getName();
    Pair<String, String> _mappedTo_1 = Pair.<String, String>of("resource", _name);
    final XtextServiceDispatcher.ServiceDescriptor validate = this.getService(Collections.<String, String>unmodifiableMap(CollectionLiterals.<String, String>newHashMap(_mappedTo, _mappedTo_1)), session);
    Pair<String, String> _mappedTo_2 = Pair.<String, String>of("serviceType", "assist");
    String _name_1 = file.getName();
    Pair<String, String> _mappedTo_3 = Pair.<String, String>of("resource", _name_1);
    Pair<String, String> _mappedTo_4 = Pair.<String, String>of("caretOffset", "0");
    final XtextServiceDispatcher.ServiceDescriptor contentAssist = this.getService(
      Collections.<String, String>unmodifiableMap(CollectionLiterals.<String, String>newHashMap(_mappedTo_2, _mappedTo_3, _mappedTo_4)), session);
    final Callable<IServiceResult> _function = () -> {
      return validate.getService().apply();
    };
    this.executorService.<IServiceResult>submit(_function);
    final Function1<UpdateDocumentTest.TestResourceValidator, Boolean> _function_1 = (UpdateDocumentTest.TestResourceValidator it) -> {
      return Boolean.valueOf((it.entryCounter == 1));
    };
    this.resourceValidator.waitUntil(_function_1);
    contentAssist.getService().apply();
    final Function1<UpdateDocumentTest.TestResourceValidator, Boolean> _function_2 = (UpdateDocumentTest.TestResourceValidator it) -> {
      return Boolean.valueOf((it.exitCounter == 1));
    };
    this.resourceValidator.waitUntil(_function_2);
    Assert.assertTrue(this.resourceValidator.canceled);
    final Function1<UpdateDocumentTest.TestResourceValidator, Boolean> _function_3 = (UpdateDocumentTest.TestResourceValidator it) -> {
      return Boolean.valueOf((it.entryCounter == 2));
    };
    this.resourceValidator.waitUntil(_function_3);
  }
  
  @Test
  public void testContentAssistWithUpdate() {
    this.resourceValidator.reset(0);
    final File file = this.createFile("input signal x state foo end");
    final HashMapSession session = new HashMapSession();
    Pair<String, String> _mappedTo = Pair.<String, String>of("serviceType", "update");
    String _name = file.getName();
    Pair<String, String> _mappedTo_1 = Pair.<String, String>of("resource", _name);
    Pair<String, String> _mappedTo_2 = Pair.<String, String>of("deltaText", "bar");
    Pair<String, String> _mappedTo_3 = Pair.<String, String>of("deltaOffset", "21");
    Pair<String, String> _mappedTo_4 = Pair.<String, String>of("deltaReplaceLength", "3");
    XtextServiceDispatcher.ServiceDescriptor update = this.getService(
      Collections.<String, String>unmodifiableMap(CollectionLiterals.<String, String>newHashMap(_mappedTo, _mappedTo_1, _mappedTo_2, _mappedTo_3, _mappedTo_4)), session);
    IServiceResult _apply = update.getService().apply();
    final DocumentStateResult updateResult = ((DocumentStateResult) _apply);
    Pair<String, String> _mappedTo_5 = Pair.<String, String>of("serviceType", "assist");
    String _name_1 = file.getName();
    Pair<String, String> _mappedTo_6 = Pair.<String, String>of("resource", _name_1);
    Pair<String, String> _mappedTo_7 = Pair.<String, String>of("caretOffset", "34");
    Pair<String, String> _mappedTo_8 = Pair.<String, String>of("deltaText", " set x = ");
    Pair<String, String> _mappedTo_9 = Pair.<String, String>of("deltaOffset", "24");
    Pair<String, String> _mappedTo_10 = Pair.<String, String>of("deltaReplaceLength", "0");
    String _stateId = updateResult.getStateId();
    Pair<String, String> _mappedTo_11 = Pair.<String, String>of("requiredStateId", _stateId);
    final XtextServiceDispatcher.ServiceDescriptor contentAssist = this.getService(
      Collections.<String, String>unmodifiableMap(CollectionLiterals.<String, String>newHashMap(_mappedTo_5, _mappedTo_6, _mappedTo_7, _mappedTo_8, _mappedTo_9, _mappedTo_10, _mappedTo_11)), session);
    contentAssist.getService().apply();
    Pair<String, String> _mappedTo_12 = Pair.<String, String>of("serviceType", "load");
    String _name_2 = file.getName();
    Pair<String, String> _mappedTo_13 = Pair.<String, String>of("resource", _name_2);
    final XtextServiceDispatcher.ServiceDescriptor load = this.getService(Collections.<String, String>unmodifiableMap(CollectionLiterals.<String, String>newHashMap(_mappedTo_12, _mappedTo_13)), session);
    IServiceResult _apply_1 = load.getService().apply();
    final ResourceContentResult loadResult = ((ResourceContentResult) _apply_1);
    Assert.assertEquals("input signal x state bar set x =  end", loadResult.getFullText());
  }
  
  @Test
  public void testNoPrecomputationInStatelessMode() {
    this.resourceValidator.reset(0);
    final File file = this.createFile("");
    final HashMapSession session = new HashMapSession();
    Pair<String, String> _mappedTo = Pair.<String, String>of("serviceType", "assist");
    String _name = file.getName();
    Pair<String, String> _mappedTo_1 = Pair.<String, String>of("resource", _name);
    Pair<String, String> _mappedTo_2 = Pair.<String, String>of("caretOffset", "6");
    Pair<String, String> _mappedTo_3 = Pair.<String, String>of("fullText", "input signal x state foo end");
    this.getService(
      Collections.<String, String>unmodifiableMap(CollectionLiterals.<String, String>newHashMap(_mappedTo, _mappedTo_1, _mappedTo_2, _mappedTo_3)), session).getService().apply();
    Pair<String, String> _mappedTo_4 = Pair.<String, String>of("serviceType", "assist");
    String _name_1 = file.getName();
    Pair<String, String> _mappedTo_5 = Pair.<String, String>of("resource", _name_1);
    Pair<String, String> _mappedTo_6 = Pair.<String, String>of("caretOffset", "6");
    Pair<String, String> _mappedTo_7 = Pair.<String, String>of("fullText", "input signal x state foo end");
    this.getService(
      Collections.<String, String>unmodifiableMap(CollectionLiterals.<String, String>newHashMap(_mappedTo_4, _mappedTo_5, _mappedTo_6, _mappedTo_7)), session).getService().apply();
    Assert.assertEquals(0, this.resourceValidator.entryCounter);
  }
}
