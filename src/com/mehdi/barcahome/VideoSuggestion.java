package com.mehdi.barcahome;
import java.net.*;
import java.util.*;

/** A local bookmark, not a scraped or personalized YouTube recommendation. */
public final class VideoSuggestion {
    public final String title, id;
    public VideoSuggestion(String title,String id) {
        if(title==null || title.trim().isEmpty() || title.length()>80 || !validId(id))throw new IllegalArgumentException();
        this.title=title.trim();this.id=id;
    }
    private static boolean validId(String id){return id!=null && id.matches("[A-Za-z0-9_-]{11}");}
    public String url(){return "https://www.youtube.com/watch?v="+id;}
    public static List<VideoSuggestion> defaults(){return new ArrayList<>(Arrays.asList(
        new VideoSuggestion("Big Buck Bunny — Blender", "aqz-KE-bpKQ"),
        new VideoSuggestion("Sintel — Blender", "eRsGyueVLvQ")));
    }
    public static String parseId(String input){
        if(input==null)return null;String value=input.trim();if(validId(value))return value;
        try{
            URI uri=new URI(value);String host=uri.getHost();
            if(!"https".equalsIgnoreCase(uri.getScheme()) || host==null || uri.getUserInfo()!=null ||
                (uri.getPort()!=-1 && uri.getPort()!=443))return null;
            host=host.toLowerCase(Locale.ROOT);String path=uri.getPath();
            if(host.equals("youtu.be")){
                String id=path!=null && path.startsWith("/")?path.substring(1):null;
                return validId(id)?id:null;
            }
            if(!Arrays.asList("youtube.com","www.youtube.com","m.youtube.com").contains(host))return null;
            if("/watch".equals(path) && uri.getRawQuery()!=null){
                for(String pair:uri.getRawQuery().split("&")){
                    String[] parts=pair.split("=",2);
                    if(parts.length==2 && URLDecoder.decode(parts[0],"UTF-8").equals("v")){
                        String id=URLDecoder.decode(parts[1],"UTF-8");return validId(id)?id:null;
                    }
                }
            }
            if(path!=null){String[] parts=path.split("/");
                if(parts.length==3 && Arrays.asList("shorts","live","embed").contains(parts[1]) && validId(parts[2]))return parts[2];}
        }catch(Exception ignored){}
        return null;
    }
}
