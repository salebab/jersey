package org.glassfish.jersey.inject.guice;

import com.google.inject.Inject;
import com.google.inject.Provider;

import java.util.function.Supplier;

/**
 * @author Aleksandar Babic
 */
public class SupplierValueProvider<S extends Supplier<T>, T> implements Provider<T> {

  @Inject
  S supplier;

  @Override
  public T get() {
    return supplier.get();
  }
}
