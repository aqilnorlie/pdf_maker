package org.example;

public class Page {

    boolean landscape = false;

    public Page(){
        this.landscape = false;
    }

    //can be flexible by pass hashmap in parameter
    public void setFormat(String detail){

        String value = detail.substring(detail.indexOf(":") + 2, detail.length() - 1).trim();
        System.out.println("set format : " + value);
        if(value.equalsIgnoreCase("size=landscape")){
            this.landscape = true;
        }


    }

}
