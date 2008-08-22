/*
 * Copyright 2008 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mockftpserver.fake.filesystem

import org.mockftpserver.core.util.IoUtil

/**
 * Tests for subclasses of AbstractFakeFileSystem. Subclasses must define
 *
 * @version $Revision$ - $Date$
 *
 * @author Chris Mair
 */
abstract class AbstractFakeFileSystemTest extends AbstractFileSystemTest {

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    void testDefaultDirectoryListingFormatterClass() {
        assert fileSystem.directoryListingFormatter.class == expectedDirectoryListingFormatterClass
    }

    void testAddEntry() {
        AbstractFakeFileSystem fakeFileSystem = (AbstractFakeFileSystem) fileSystem
        assertFalse("exists before", fileSystem.exists(NEW_FILE))
        fakeFileSystem.addEntry(new DirectoryEntry(NEW_DIR))
        assertTrue("/ exists after", fileSystem.exists(NEW_DIR))
        fakeFileSystem.addEntry(new FileEntry(NEW_FILE))
        assertTrue("exists after", fileSystem.exists(NEW_FILE))

        // Try adding entry that already exists
        shouldFail(FileSystemException) { fakeFileSystem.addEntry(new FileEntry(NEW_FILE)) }

        // Try adding entry for path whose parent does not already exist
        shouldFail(FileSystemException) { fakeFileSystem.addEntry(new FileEntry("/abc/def/ghi")) }
    }

    void testCreateDirectory_CreateParentDirectoriesAutomatically() {
        final NEW_SUBDIR = fileSystem.path(NEW_DIR, "sub")
        fileSystem.createParentDirectoriesAutomatically = true
        assert !fileSystem.exists(NEW_DIR), "Before createDirectory"
        assert !fileSystem.exists(NEW_SUBDIR), "Before createDirectory"

        assert fileSystem.createDirectory(NEW_SUBDIR)
        assert fileSystem.exists(NEW_DIR), "After createDirectory"
        assert fileSystem.exists(NEW_SUBDIR), "$NEW_SUBDIR: After createDirectory"
    }

    void testCreateFile_CreateParentDirectoriesAutomatically() {
        final NEW_FILE_IN_SUBDIR = fileSystem.path(NEW_DIR, "abc.txt")
        fileSystem.createParentDirectoriesAutomatically = true
        assert !fileSystem.exists(NEW_DIR), "Before createDirectory"
        assert !fileSystem.exists(NEW_FILE_IN_SUBDIR), "Before createDirectory"

        assert fileSystem.createFile(NEW_FILE_IN_SUBDIR)
        assert fileSystem.exists(NEW_DIR), "After createDirectory"
        assert fileSystem.exists(NEW_FILE_IN_SUBDIR), "$NEW_FILE_IN_SUBDIR: After createDirectory"
    }

    void testToString() {
        String toString = fileSystem.toString()
        LOG.info("toString=" + toString)
        assert toString.contains(fileSystem.normalize(EXISTING_DIR))
        assert toString.contains(fileSystem.normalize(EXISTING_FILE))
    }

    void testFormatDirectoryListing() {
        def fileInfo = FileInfo.forDirectory('abc', null)
        def formatter = [format: {f ->
            assert f == fileInfo
            return 'abc'
        }] as DirectoryListingFormatter
        fileSystem.directoryListingFormatter = formatter
        assert fileSystem.formatDirectoryListing(fileInfo) == 'abc'
    }

    void testFormatDirectoryListing_NullDirectoryListingFormatter() {
        fileSystem.directoryListingFormatter = null
        def fileInfo = FileInfo.forDirectory('abc', null)
        shouldFailWithMessageContaining('directoryListingFormatter') { assert fileSystem.formatDirectoryListing(fileInfo) }
    }

    void testFormatDirectoryListing_NullFileInfo() {
        def formatter = [format: {f -> }] as DirectoryListingFormatter
        fileSystem.directoryListingFormatter = formatter
        shouldFailWithMessageContaining('fileInfo') { assert fileSystem.formatDirectoryListing(null) }
    }

    void testGetFileInfo() {
        assert fileSystem.getFileInfo(NO_SUCH_DIR) == null
        assert fileSystem.getFileInfo(NO_SUCH_FILE) == null

        assert fileSystem.getFileInfo(EXISTING_FILE).name == fileSystem.getName(EXISTING_FILE)
        assert fileSystem.getFileInfo(EXISTING_DIR).name == fileSystem.getName(EXISTING_DIR)

        def permissions = new Permissions('-wxrwx---')
        def fileEntry = new FileEntry(path: NEW_FILE, lastModified: DATE, contents: 'abc', owner: 'owner',
                group: 'group', permissions: permissions)
        fileSystem.addEntry(fileEntry)
        def fileInfo = fileSystem.getFileInfo(NEW_FILE)
        LOG.info(fileInfo)
        assert fileInfo.name == fileSystem.getName(NEW_FILE)
        assert !fileInfo.directory
        assert fileInfo.size == 3
        assert fileInfo.owner == 'owner'
        assert fileInfo.group == 'group'
        assert fileInfo.permissions == permissions
    }

    //--------------------------------------------------------------------------
    // Abstract Methods
    //--------------------------------------------------------------------------

    protected abstract Class getExpectedDirectoryListingFormatterClass()

    //--------------------------------------------------------------------------
    // Internal Helper Methods
    //--------------------------------------------------------------------------

    /**
     * Verify the contents of the file at the specified path read from its InputSteam
     *
     * @param fileSystem - the FileSystem instance
     * @param expectedContents - the expected contents
     * @throws IOException
     * @see org.mockftpserver.fake.filesystem.AbstractFileSystemTest#verifyFileContents(FileSystem,
     * String , String )
     */
    protected void verifyFileContents(FileSystem fileSystem, String path, String expectedContents) throws IOException {
        InputStream input = fileSystem.createInputStream(path)
        byte[] bytes = IoUtil.readBytes(input)
        LOG.info("bytes=[" + new String(bytes) + "]")
        assertEquals("contents: actual=[" + new String(bytes) + "]", expectedContents.getBytes(), bytes)
    }

}