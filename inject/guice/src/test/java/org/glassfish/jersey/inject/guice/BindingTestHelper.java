package org.glassfish.jersey.inject.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.Injections;

import java.util.function.Consumer;

/**
 * @author Aleksandar Babic
 */
public class BindingTestHelper {

  /**
   * Accepts the provided consumer to created and register the binder.
   *
   * @param injectionManager injection manager which accepts the consumer.
   * @param bindConsumer     consumer to populate a binder.
   */
  static void bind(InjectionManager injectionManager, Consumer<AbstractBinder> bindConsumer) {
    AbstractBinder binder = new AbstractBinder() {
      @Override
      protected void configure() {
        bindConsumer.accept(this);
      }
    };

    injectionManager.register(binder);
    injectionManager.completeRegistration();
  }

  public static InjectionManager createInjectionManager() {
    return Injections.createInjectionManager();
  }
}
