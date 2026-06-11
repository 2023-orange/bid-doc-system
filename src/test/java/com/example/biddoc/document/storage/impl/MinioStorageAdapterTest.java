package com.example.biddoc.document.storage.impl;

import com.example.biddoc.document.storage.config.MinioProperties;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinioStorageAdapterTest {

    @Test
    void generateStorageKeyDoesNotAddSecondDotWhenHintExtensionAlreadyContainsDot() throws Exception {
        MinioStorageAdapter adapter = new MinioStorageAdapter(new MinioProperties());

        Method method = MinioStorageAdapter.class.getDeclaredMethod("generateStorageKey", String.class);
        method.setAccessible(true);
        String storageKey = (String) method.invoke(adapter, ".pdf");

        assertTrue(storageKey.endsWith(".pdf"));
        assertFalse(storageKey.contains(".."));
    }
}
