package org.sonar.tests;

public class ClassI {
  public String hello;
  
  public ClassI(String s){
    this.hello = s;
  }
  
  public String say() {
  	return hello;
  }
}
