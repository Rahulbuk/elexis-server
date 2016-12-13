package info.elexis.server.core.connector.elexis.services;

import static org.junit.Assert.*;

import org.junit.Test;

import ch.elexis.core.lock.types.LockInfo;
import ch.elexis.core.lock.types.LockResponse;
import ch.elexis.core.lock.types.LockResponse.Status;

public class LockServiceTest {

	@Test
	public void testLockResponseInvariants() {
		LockResponse permDeny = new LockResponse(Status.DENIED_PERMANENT, new LockInfo());
		assertFalse(permDeny.isOk());
		LockResponse denied = LockResponse.DENIED(new LockInfo());
		assertFalse(denied.isOk());
		LockResponse error = LockResponse.ERROR;
		assertFalse(error.isOk());
		LockResponse ok = LockResponse.OK();
		assertTrue(ok.isOk());
	}

	@Test
	public void testAcquireLock() {
		LockService service = new LockService();
		LockInfo lockInfo = new LockInfo("objStoreToString::1", "objUser", "testUuid");
		LockResponse lockResponse = service.acquireLock(lockInfo);

		assertNotNull(lockResponse);
		assertTrue(lockResponse.isOk());
		lockResponse = service.releaseLock(lockInfo);
	}

	@Test
	public void testReleaseLock() {
		LockService service = new LockService();
		LockInfo lockInfo = new LockInfo("objStoreToString::1", "objUser", "testUuid");
		LockResponse lockResponse = service.acquireLock(lockInfo);

		lockResponse = service.releaseLock(lockInfo);
		assertNotNull(lockResponse);
		assertTrue(lockResponse.isOk());
		assertFalse(service.isLocked(lockInfo));
	}

	@Test
	public void testDoubleAcquire() {
		LockService service = new LockService();
		LockInfo lockInfo = new LockInfo("objStoreToString::1", "objUser", "testUuid");

		LockResponse lockResponse = service.acquireLock(lockInfo);
		assertTrue(lockResponse.isOk());
		assertTrue(service.isLocked(lockInfo));

		lockResponse = service.acquireLock(lockInfo);
		assertNotNull(lockResponse);
		assertTrue(lockResponse.isOk());
		assertTrue(service.isLocked(lockInfo));

		lockResponse = service.releaseLock(lockInfo);
	}

	@Test
	public void testAcquireLocked() {
		LockService service = new LockService();
		LockInfo lockInfo1 = new LockInfo("objStoreToString::1", "objUser", "testUuid1");
		LockInfo lockInfo2 = new LockInfo("objStoreToString::1", "objUser", "testUuid2");

		LockResponse lockResponse = service.acquireLock(lockInfo1);
		assertTrue(lockResponse.isOk());
		assertTrue(service.isLocked(lockInfo1));

		lockResponse = service.acquireLock(lockInfo2);
		assertNotNull(lockResponse);
		assertFalse(lockResponse.isOk());
		assertFalse(service.isLocked(lockInfo2));

		lockResponse = service.releaseLock(lockInfo1);
	}

	@Test
	public void testDoubleRelease() {
		LockService service = new LockService();
		LockInfo lockInfo = new LockInfo("objStoreToString::1", "objUser", "testUuid");
		LockResponse lockResponse = service.acquireLock(lockInfo);

		lockResponse = service.releaseLock(lockInfo);
		assertNotNull(lockResponse);
		assertTrue(lockResponse.isOk());
		assertFalse(service.isLocked(lockInfo));

		lockResponse = service.releaseLock(lockInfo);
		assertNotNull(lockResponse);
		assertFalse(lockResponse.isOk());
		assertFalse(service.isLocked(lockInfo));
	}
	
	@Test
	public void testAcquireLockBlocking() {
		final LockService service = new LockService();
		LockInfo lockInfo1 = new LockInfo("objStoreToString::11", "objUserA", "testUuid1");
		LockInfo lockInfo2 = new LockInfo("objStoreToString::11", "objUserB", "testUuid2");
		
		LockResponse lockResponse = service.acquireLock(lockInfo1);
		assertTrue(lockResponse.isOk());
		Thread thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					Thread.sleep(500);
					service.releaseLock(lockInfo1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		thread.start();
		
		LockResponse al1 = service.acquireLock(lockInfo2);
		assertFalse(al1.isOk());
		
		long start = System.currentTimeMillis();
		LockResponse lockResponse2 = service.acquireLockBlocking(lockInfo2, 5);
		long end = System.currentTimeMillis();
		assertTrue(end-start > 500);
		assertTrue(lockResponse2.isOk());
		assertFalse(thread.isAlive());
	}
}
