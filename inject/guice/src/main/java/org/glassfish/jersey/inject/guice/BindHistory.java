package org.glassfish.jersey.inject.guice;

import com.google.inject.Key;

import javax.inject.Singleton;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Aleksandar Babic
 */
@Singleton
public class BindHistory {

  private final Set<Key> boundKeys = ConcurrentHashMap.newKeySet();

  boolean exists(Key key) {
    return boundKeys.contains(key);
  }

  void add(Key key) {
    boundKeys.add(key);
  }
}
