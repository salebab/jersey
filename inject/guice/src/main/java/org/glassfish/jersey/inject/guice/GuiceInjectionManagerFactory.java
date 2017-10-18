package org.glassfish.jersey.inject.guice;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.InjectionManagerFactory;

/**
 * @author Aleksandar Babic
 */
public class GuiceInjectionManagerFactory implements InjectionManagerFactory {

  @Override
  public InjectionManager create(Object parent) {
    Injector injector = resolveInjectorParent(parent);
    return new GuiceInjectionManager(injector);
  }

  @Override
  public InjectionManager create() {
    return new GuiceInjectionManager(Guice.createInjector());
  }

  private static void assertParentLocatorType(Object parent) {
    if (parent != null && !(parent instanceof Injector || parent instanceof GuiceInjectionManager)) {
      throw new IllegalArgumentException("Guice - Unknown Parent injection Manager " + parent.getClass().getSimpleName());
    }
  }

  private static Injector resolveInjectorParent(Object parent) {
    assertParentLocatorType(parent);

    Injector parentLocator = null;
    if (parent != null) {
      if (parent instanceof Injector) {
        parentLocator = (Injector) parent;
      } else if (parent instanceof GuiceInjectionManager) {
        parentLocator = ((GuiceInjectionManager) parent).getInjector();
      }
    }
    return parentLocator;
  }
}
