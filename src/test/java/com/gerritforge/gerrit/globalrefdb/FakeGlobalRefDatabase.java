// Copyright (C) 2019 GerritForge Ltd
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.gerritforge.gerrit.globalrefdb;

import com.google.common.collect.MapMaker;
import com.google.gerrit.entities.Project;
import com.google.gerrit.entities.Project.NameKey;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.junit.Ignore;

@Ignore
public class FakeGlobalRefDatabase implements ExtendedGlobalRefDatabase {

  private ConcurrentMap<Project.NameKey, ConcurrentMap<String, AtomicReference<ObjectId>>>
      keyValueStore;
  private ConcurrentMap<String, AtomicReference<?>> genericKeyValueStore;

  private ConcurrentMap<Project.NameKey, ConcurrentMap<String, AtomicReference<Lock>>> refLockStore;

  public FakeGlobalRefDatabase() {
    keyValueStore = new MapMaker().concurrencyLevel(1).makeMap();
    refLockStore = new MapMaker().concurrencyLevel(1).makeMap();
    genericKeyValueStore = new MapMaker().concurrencyLevel(1).makeMap();
  }

  @Override
  public boolean isUpToDate(Project.NameKey project, Ref ref) throws GlobalRefDbLockException {
    AtomicReference<ObjectId> value = projectRefDb(project).get(ref.getName());
    if (value == null) {
      return true;
    }
    return ref.getObjectId().equals(value.get());
  }

  @Override
  public boolean compareAndPut(Project.NameKey project, Ref currRef, ObjectId newRefValue)
      throws GlobalRefDbSystemError {
    ConcurrentMap<String, AtomicReference<ObjectId>> projectRefDb = projectRefDb(project);
    AtomicReference<ObjectId> currValue = projectRefDb.get(currRef.getName());
    if (currValue == null) {
      projectRefDb.put(currRef.getName(), new AtomicReference<>(newRefValue));
      return true;
    }

    return currValue.compareAndSet(currRef.getObjectId(), newRefValue);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> boolean compareAndPut(Project.NameKey project, String refName, T currValue, T newValue)
      throws GlobalRefDbSystemError {
    String key = String.format("%s/%s", project.get(), refName);
    AtomicReference<T> storedValue = (AtomicReference<T>) genericKeyValueStore.get(key);
    if (storedValue == null) {
      genericKeyValueStore.put(key, new AtomicReference<>(newValue));
      return true;
    }

    return storedValue.compareAndSet(currValue, newValue);
  }

  @Override
  public AutoCloseable lockRef(Project.NameKey project, String refName)
      throws GlobalRefDbLockException {
    ConcurrentMap<String, AtomicReference<Lock>> projectRefLock = projectRefLock(project);
    AtomicReference<Lock> currLock = projectRefLock.get(refName);
    if (currLock == null) {
      Lock lock = new ReentrantLock();
      lock.lock();
      projectRefLock.put(refName, new AtomicReference<>(lock));
      return new RefLock(lock);
    }

    Lock lock = currLock.get();
    lock.lock();
    return new RefLock(lock);
  }

  @Override
  public boolean exists(Project.NameKey project, String refName) {
    return projectRefDb(project).containsKey(refName);
  }

  @Override
  public void remove(Project.NameKey project) throws GlobalRefDbSystemError {
    keyValueStore.remove(project);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Optional<T> get(Project.NameKey project, String refName, Class<T> clazz)
      throws GlobalRefDbSystemError {
    String key = String.format("%s/%s", project.get(), refName);
    return Optional.ofNullable((T) genericKeyValueStore.get(key))
        .map(v -> ((AtomicReference<T>) v).get());
  }

  private ConcurrentMap<String, AtomicReference<ObjectId>> projectRefDb(Project.NameKey project) {
    ConcurrentMap<String, AtomicReference<ObjectId>> projectRefDb = keyValueStore.get(project);
    if (projectRefDb == null) {
      projectRefDb = new MapMaker().concurrencyLevel(1).makeMap();
      keyValueStore.put(project, projectRefDb);
    }

    return projectRefDb;
  }

  private ConcurrentMap<String, AtomicReference<Lock>> projectRefLock(Project.NameKey project) {
    ConcurrentMap<String, AtomicReference<Lock>> projectRefLock = refLockStore.get(project);
    if (projectRefLock == null) {
      projectRefLock = new MapMaker().concurrencyLevel(1).makeMap();
      refLockStore.put(project, projectRefLock);
    }

    return projectRefLock;
  }

  @Override
  public <T> void put(NameKey project, String refName, T newValue) throws GlobalRefDbSystemError {
    String key = String.format("%s/%s", project.get(), refName);
    genericKeyValueStore.put(key, new AtomicReference<>(newValue));
  }

  private static class RefLock implements AutoCloseable {
    private Lock lock;

    public RefLock(Lock lock) {
      this.lock = lock;
    }

    @Override
    public void close() throws Exception {
      lock.unlock();
    }
  }
}
