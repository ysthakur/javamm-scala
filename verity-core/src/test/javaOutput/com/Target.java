package com;import java.lang.*;public class Target { public Target () {} public static void main ( String args) {}} class Person { public Person ( String n, int a) {name=n;age=a;}} class SetName {  SetName () {}} class SetAge {  SetAge () {}} class PersonBuilder {  PersonBuilder () {} public static Person build ( BuildPerson bp) {bp.buildPerson(this);return Person(name,age);} public void setName ( String n) {name=n;proof SetNameev=null;return ;} public void setAge ( int a) {age=a;proof SetAgeev=null;return ;}}abstract class BuildPerson {  BuildPerson () {} abstract void buildPerson ( PersonBuilder p) ;} class BuildFoo {  BuildFoo () {}  void buildPerson ( PersonBuilder p) {p.setName("Foo")(null);p.setAge(0)(null);return ;}}