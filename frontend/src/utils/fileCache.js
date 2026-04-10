const FILE_CACHE_DB = "sortify-file-cache";
const FILE_CACHE_STORE = "uploads";
const FILE_CACHE_KEY = "selected-files";

function openFileCache() {
  return new Promise((resolve, reject) => {
    const request = window.indexedDB.open(FILE_CACHE_DB, 1);

    request.onupgradeneeded = () => {
      const database = request.result;
      if (!database.objectStoreNames.contains(FILE_CACHE_STORE)) {
        database.createObjectStore(FILE_CACHE_STORE, { keyPath: "id" });
      }
    };

    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

export async function saveCachedFiles(files) {
  if (!window.indexedDB) {
    return;
  }

  const database = await openFileCache();

  await new Promise((resolve, reject) => {
    const transaction = database.transaction(FILE_CACHE_STORE, "readwrite");
    transaction.objectStore(FILE_CACHE_STORE).put({ id: FILE_CACHE_KEY, files });
    transaction.oncomplete = () => resolve();
    transaction.onerror = () => reject(transaction.error);
  });

  database.close();
}

export async function loadCachedFiles() {
  if (!window.indexedDB) {
    return [];
  }

  const database = await openFileCache();

  const result = await new Promise((resolve, reject) => {
    const transaction = database.transaction(FILE_CACHE_STORE, "readonly");
    const request = transaction.objectStore(FILE_CACHE_STORE).get(FILE_CACHE_KEY);
    request.onsuccess = () => resolve(request.result?.files || []);
    request.onerror = () => reject(request.error);
  });

  database.close();
  return result;
}

export async function clearCachedFiles() {
  if (!window.indexedDB) {
    return;
  }

  const database = await openFileCache();

  await new Promise((resolve, reject) => {
    const transaction = database.transaction(FILE_CACHE_STORE, "readwrite");
    transaction.objectStore(FILE_CACHE_STORE).delete(FILE_CACHE_KEY);
    transaction.oncomplete = () => resolve();
    transaction.onerror = () => reject(transaction.error);
  });

  database.close();
}
