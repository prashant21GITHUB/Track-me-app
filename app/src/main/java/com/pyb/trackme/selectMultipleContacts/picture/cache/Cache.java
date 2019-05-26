

package com.pyb.trackme.selectMultipleContacts.picture.cache;

public interface Cache<K, V> {

    void put(K key, V value);

    V get(K key);

    void evictAll();

}
