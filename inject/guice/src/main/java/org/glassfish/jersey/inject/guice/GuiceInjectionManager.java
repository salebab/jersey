package org.glassfish.jersey.inject.guice;

import com.google.inject.Injector;
import org.glassfish.jersey.internal.inject.*;
import org.glassfish.jersey.internal.util.ExtendedLogger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Aleksandar Babic
 */
public class GuiceInjectionManager implements InjectionManager {

  private static final ExtendedLogger logger =
      new ExtendedLogger(Logger.getLogger(GuiceInjectionManager.class.getName()), Level.FINEST);

  private Injector childInjector;
  private final Injector injector;
  private final JerseyBindingsModule module;

  public GuiceInjectionManager(Injector injector) {
    logger.debugLog("Instanced.");

    this.injector = injector;
    this.module = new JerseyBindingsModule();
  }

  @Override
  public void completeRegistration() {
    logger.debugLog("Registration is completed.");

    childInjector = injector.createChildInjector(module);
  }

  @Override
  public void shutdown() {
    logger.debugLog("Shutdown.");
  }

  @Override
  public void register(Binding binding) {
    logger.debugLog("Registering binding. Contracts: {0}; Implementations: {1}",  binding.getContracts(), binding.getImplementationType());

    if(binding.getImplementationType() == null) {
      logger.debugLog("Implementation type NULL");
    }

    module.addBinding(binding);
  }

  @Override
  public void register(Iterable<Binding> descriptors) {
    logger.debugLog("Register descriptors {0}", descriptors);

    descriptors.forEach(this::register);
  }

  @Override
  public void register(Binder binder) {
    logger.debugLog("START: Register each biding from binder {0}", binder);

    binder.getBindings().forEach(this::register);

    logger.debugLog("END: Register each biding from binder {0}", binder);
  }

  @Override
  public void register(Object provider) throws IllegalArgumentException {
    throw new IllegalArgumentException("Not supported.");
  }

  @Override
  public boolean isRegistrable(Class<?> clazz) {
    return false;
  }

  @Override
  public <T> T createAndInitialize(Class<T> createMe) {
    logger.debugLog("Creating and initializing {0}", createMe);
    return injector.getInstance(createMe);
  }

  @Override
  public <T> List<ServiceHolder<T>> getAllServiceHolders(Class<T> contractOrImpl, Annotation... qualifiers) {
    return null;
  }

  @Override
  public <T> T getInstance(Class<T> contractOrImpl, Annotation... qualifiers) {
    return null;
  }

  @Override
  public <T> T getInstance(Class<T> contractOrImpl, String classAnalyzer) {
    return null;
  }

  @Override
  public <T> T getInstance(Class<T> contractOrImpl) {
    logger.debugLog("Getting instance from childInjector for class '{0}'", contractOrImpl);

    return childInjector.getInstance(contractOrImpl);
  }

  @Override
  public <T> T getInstance(Type contractOrImpl) {
    logger.debugLog("Getting instance from childInjector for type '{0}'", contractOrImpl);

    return childInjector.getInstance((Class<T>) contractOrImpl);
  }

  @Override
  public Object getInstance(ForeignDescriptor foreignDescriptor) {
    return null;
  }

  @Override
  public ForeignDescriptor createForeignDescriptor(Binding binding) {
    return null;
  }

  @Override
  public <T> List<T> getAllInstances(Type contractOrImpl) {
    logger.debugLog("Getting all instances for type '{0}'", contractOrImpl);
    return null;
  }

  @Override
  public void inject(Object injectMe) {
    logger.debugLog("Inject {0}", injectMe);
  }

  @Override
  public void inject(Object injectMe, String classAnalyzer) {

  }

  @Override
  public void preDestroy(Object preDestroyMe) {

  }
}
