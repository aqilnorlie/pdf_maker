package org.example;

import java.util.HashMap;
import java.util.Map;

public class Page {

    boolean landscape = false;

    public Page(){
        this.landscape = false;
    }

    //can be flexible by pass hashmap in parameter
    public void setFormat(){

        this.landscape = true;

    }

}
