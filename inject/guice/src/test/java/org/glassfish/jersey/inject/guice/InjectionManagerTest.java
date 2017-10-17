package org.glassfish.jersey.inject.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.glassfish.jersey.internal.inject.Bindings;
import org.glassfish.jersey.internal.inject.ClassBinding;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.Injections;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Aleksandar Babic
 */
public class InjectionManagerTest {

  @Test
  public void testInjectorParent() {
    AbstractModule module = new AbstractModule() {
      @Override
      protected void configure() {
        bind(EnglishGreeting.class).asEagerSingleton();
      }
    };
    Injector injector = Guice.createInjector(module);

    InjectionManager injectionManager = Injections.createInjectionManager(injector);
    injectionManager.completeRegistration();

    EnglishGreeting greeting1 = injectionManager.getInstance(EnglishGreeting.class);
    EnglishGreeting greeting2 = injectionManager.getInstance(EnglishGreeting.class);

    assertNotNull(greeting1);
    assertNotNull(greeting2);
    assertEquals(greeting1, greeting2); // singleton
  }

  @Test
  public void testInjectionManagerParent() {
    ClassBinding<EnglishGreeting> greetingBinding = Bindings.serviceAsContract(EnglishGreeting.class);
    InjectionManager parentInjectionManager = Injections.createInjectionManager();
    parentInjectionManager.register(greetingBinding);
    parentInjectionManager.completeRegistration();

    InjectionManager injectionManager = Injections.createInjectionManager(parentInjectionManager);
    injectionManager.completeRegistration();
    assertNotNull(injectionManager.getInstance(EnglishGreeting.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testUnknownParent() {
    Injections.createInjectionManager(new Object());
  }

  @Test
  public void testIsRegistrable() {
    InjectionManager injectionManager = Injections.createInjectionManager();
    assertTrue(injectionManager.isRegistrable(Module.class));
    assertTrue(injectionManager.isRegistrable(AbstractModule.class));
    assertFalse(injectionManager.isRegistrable(org.glassfish.jersey.internal.inject.AbstractBinder.class));
    assertFalse(injectionManager.isRegistrable(String.class));
  }

  @Test
  public void testRegisterBinder() {
    AbstractModule module = new AbstractModule() {
      @Override
      protected void configure() {
        bind(EnglishGreeting.class).asEagerSingleton();
      }
    };

    InjectionManager injectionManager = Injections.createInjectionManager();
    injectionManager.register(module);
    injectionManager.completeRegistration();
    assertNotNull(injectionManager.getInstance(EnglishGreeting.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRegisterUnknownProvider() {
    InjectionManager injectionManager = Injections.createInjectionManager();
    injectionManager.register(new Object());
  }
}
