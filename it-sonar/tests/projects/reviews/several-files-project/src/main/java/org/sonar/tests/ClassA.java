package org.sonar.tests;

public class ClassA {
  public String hello;
  
  public ClassA(String s){
    this.hello = s;
  }
  
  public String say() {
  	return hello;
  }
}
