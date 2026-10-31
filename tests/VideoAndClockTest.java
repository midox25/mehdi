import com.mehdi.barcahome.*;
import java.util.*;

public final class VideoAndClockTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        String id="aqz-KE-bpKQ";
        check(id.equals(VideoSuggestion.parseId("https://www.youtube.com/watch?v="+id+"&feature=share")),"Canonical YouTube URL");
        check(id.equals(VideoSuggestion.parseId("https://youtu.be/"+id+"?si=example")),"Shared short URL");
        check(id.equals(VideoSuggestion.parseId("https://m.youtube.com/shorts/"+id)),"Mobile Shorts URL");
        check(id.equals(VideoSuggestion.parseId(id)),"Raw ID accepted without changing destination");
        for(String bad:Arrays.asList("https://youtube.com.evil.test/watch?v="+id,"https://user@youtube.com/watch?v="+id,
            "javascript:alert(1)","http://youtube.com/watch?v="+id,"https://youtube.com:8443/watch?v="+id,
            "https://youtu.be/not-valid","https://www.youtube.com/watch?v=bad%26redirect=elsewhere"))
            check(VideoSuggestion.parseId(bad)==null,"Reject invalid or non-YouTube link: "+bad);
        List<VideoSuggestion> defaults=VideoSuggestion.defaults();
        check(defaults.size()==2 && !defaults.get(0).id.equals(defaults.get(1).id),"Two distinct local suggestions");
        check(defaults.get(0).url().equals("https://www.youtube.com/watch?v="+id),"Launch only canonical HTTPS watch URL");
        boolean invalid=false;try{new VideoSuggestion(" ",id);}catch(IllegalArgumentException e){invalid=true;}
        check(invalid,"Empty video title rejected");
        TimeZone utc=TimeZone.getTimeZone("UTC");
        check(ClockDisplay.format(14*3600000L+45*60000L,utc,-60).equals("13:45"),"Minus one hour");
        check(ClockDisplay.format(15*60000L,utc,-60).equals("23:15"),"Midnight wraps to previous day");
        check(ClockDisplay.format(14*3600000L+45*60000L,utc,0).equals("14:45"),"Offset can be undone");
        check(ClockDisplay.format(13*3600000L,TimeZone.getTimeZone("GMT+01:00"),-60).equals("13:00"),"Use TV timezone with display offset");
        System.out.println("PASS: "+checks+" video-link and clock checks");
    }
}
