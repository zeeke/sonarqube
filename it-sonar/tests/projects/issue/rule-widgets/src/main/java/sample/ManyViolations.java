package sample;

import java.io.Serializable;

public class ManyViolations implements Serializable {

	public ManyViolations(int i) {
		int j = i++;
	}

  public ManyViolations(long i) {
		long j = i++;
	}

	private String myMethod() {
		return "hello";
	}

  private String myMethod() {
    if (true)
		  return "hello";
    return "foo";
	}
}// do not set a newline on the last line