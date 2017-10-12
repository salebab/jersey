package org.glassfish.jersey.inject.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import org.glassfish.jersey.internal.inject.Binding;
import org.glassfish.jersey.process.internal.RequestScope;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Aleksandar Babic
 */
public class JerseyBindingsModule extends AbstractModule {

  List<org.glassfish.jersey.internal.inject.Binding> bindings = new ArrayList<>();

  @Override
  protected void configure() {
    bind(RequestScope.class).to(GuiceRequestScope.class).in(Singleton.class);

    // iterate and register bindings in Guice
  }

  public void addBinding(Binding binding) {
    bindings.add(binding);
  }

}
