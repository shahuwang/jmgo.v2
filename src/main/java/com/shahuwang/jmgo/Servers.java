package com.shahuwang.jmgo;

import org.bson.BsonDocument;
import org.bson.BsonElement;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

/**
 * Created by rickey on 2017/3/1.
 */
public class Servers {
    private List<MongoServer> slice;

    public MongoServer search(ServerAddr addr){
        for(MongoServer server: this.slice){
            if (server.getAddr().getTcpaddr().equals(addr.getTcpaddr())){
                return server;
            }
        }
        return null;
    }

    public void add(MongoServer server){
        this.slice.add(server);
        this.slice.sort(new ServerComparator());
    }

    public MongoServer remove(MongoServer other){
        MongoServer server = this.search(other.getAddr());
        if (server != null){
            this.slice.remove(server);
        }
        return server;
    }

    public MongoServer get(int i){
        return this.slice.get(i);
    }

    public int len() {
        return this.slice.size();
    }

    public boolean empty() {
        return this.slice.size() == 0;
    }

    public boolean hasMongos() {
        for(MongoServer server: this.slice){
            if (server.getInfo().isMongos()){
                return true;
            }
        }
        return false;
    }

    public MongoServer[] getSlice() {
        return slice.toArray(new MongoServer[slice.size()]);
    }

    public MongoServer bestFit(Mode mode, BsonDocument serverTags) {
        MongoServer best = null;
        for(MongoServer next: this.slice){
            if(best == null){
                best = next;
                best.getRwlock().readLock().lock();
                if(serverTags != null && !next.getInfo().isMongos() && !best.hasTags(serverTags)){
                    best.getRwlock().readLock().unlock();
                    best = null;
                }
                continue;
            }
            next.getRwlock().readLock().lock();
            boolean swap = false;
            if((serverTags != null && !next.getInfo().isMongos() && !next.hasTags(serverTags))
                    || (mode == Mode.SECONDARY && next.getInfo().isMaster() && !next.getInfo().isMongos())
                    || (next.getInfo().isMaster() != best.getInfo().isMaster() && mode != Mode.NEAREST)){
                swap = (mode == Mode.PRIMARY_PREFERRED) != best.getInfo().isMaster();
            }
            if (next.getPingValue().abs().compareTo(Duration.ofMillis(15)) > 0){
                swap = next.getPingValue().compareTo(best.getPingValue()) < 0;
            }
            if (next.getLiveSockets().size() - next.getUnusedSockets().size() < best.getLiveSockets().size() - best.getUnusedSockets().size()){
                swap = true;
            }
            if (swap){
                best.getRwlock().readLock().unlock();
                best = next;
            } else {
                next.getRwlock().readLock().unlock();
            }
        }
        if (best != null) {
            // 为什么要做这一步呢
            best.getRwlock().readLock().unlock();
        }
        return best;
    }

    class ServerComparator implements Comparator<MongoServer>{
        public int compare(MongoServer s1, MongoServer s2){
            String host1 = s1.getAddr().getTcpaddr().getAddress().getCanonicalHostName();
            String host2 = s2.getAddr().getTcpaddr().getAddress().getCanonicalHostName();
            return host1.compareTo(host2);
        }
    }
}
