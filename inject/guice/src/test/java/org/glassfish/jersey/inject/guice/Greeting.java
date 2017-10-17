package org.glassfish.jersey.inject.guice;

/**
 * @author Aleksandar Babic
 */
@FunctionalInterface
public interface Greeting {

  /**
   * Returns greeting in a specific language.
   *
   * @return type of the greeting.
   */
  String getGreeting();
}
