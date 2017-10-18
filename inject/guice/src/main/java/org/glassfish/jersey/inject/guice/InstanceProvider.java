package org.glassfish.jersey.inject.guice;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;

/**
 * @author Aleksandar Babic
 */
public class InstanceProvider<T> implements Provider<T> {

  @Inject
  private Injector injector;
  private final Class<T> supplierClass;

  public InstanceProvider(Class<T> supplierClass) {
    this.supplierClass = supplierClass;
  }

  @Override
  public T get() {
    return injector.getInstance(supplierClass);
  }
}
