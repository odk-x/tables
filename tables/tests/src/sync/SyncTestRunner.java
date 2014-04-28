package sync;

import java.io.File;

import org.junit.runners.model.InitializationError;

import com.xtremelabs.robolectric.RobolectricTestRunner;

public class SyncTestRunner extends RobolectricTestRunner {

  public SyncTestRunner(Class<?> testClass) throws InitializationError {
    super(testClass, new File(".").getAbsoluteFile().getParentFile().getParentFile());
  }
}
