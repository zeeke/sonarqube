package org.sonar.tests;

public class ClassB {
  public String hello;
  
  public ClassB(String s){
    this.hello = s;
  }
  
  public String say() {
  	return hello;
  }
}
