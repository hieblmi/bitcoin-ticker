package com.hieblmi.btcticker;

import java.sql.Timestamp;

class TickerUpdate implements Comparable<TickerUpdate> {
    private String price;
    private String last_size;
    private String side;
    private Timestamp time;
    private String type;

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getSize() {
        return last_size;
    }

    public void setSize(String last_size) {
        this.last_size = last_size;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public Timestamp getTimestamp() {
        return time;
    }

    public void setTimestamp(Timestamp time) {
        this.time = time;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public int compareTo(TickerUpdate o) {
        return time.compareTo(o.getTimestamp());
    }

    @Override
    public String toString() {
        return "TickerUpdate{" +
                "price='" + price + '\'' +
                ", size='" + last_size + '\'' +
                ", timestamp=" + time +
                '}';
    }
}
