package net.gtaun.updater;

public class Main {
    public static void main(String[] args) {
        Updater updater = new Updater();
        if(args.length == 0) updater.update(true);
        else if(args.length >= 1) updater.update(false);
    }
}
