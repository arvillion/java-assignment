import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This is just a demo for you, please run it on JDK17.
 * This is just a demo, and you can extend and implement functions
 * based on this demo, or implement it in a different way.
 */
public class OnlineCoursesAnalyzer {

  List<Course> courses = new ArrayList<>();

  public OnlineCoursesAnalyzer(String datasetPath) {
    BufferedReader br = null;
    String line;
    try {
      br = new BufferedReader(new FileReader(datasetPath, StandardCharsets.UTF_8));
      br.readLine();
      while ((line = br.readLine()) != null) {
        String[] info = line.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);
        Course course = new Course(info[0], info[1], new Date(info[2]), info[3], info[4], info[5],
            Integer.parseInt(info[6]), Integer.parseInt(info[7]), Integer.parseInt(info[8]),
            Integer.parseInt(info[9]), Integer.parseInt(info[10]), Double.parseDouble(info[11]),
            Double.parseDouble(info[12]), Double.parseDouble(info[13]),
            Double.parseDouble(info[14]),
            Double.parseDouble(info[15]), Double.parseDouble(info[16]),
            Double.parseDouble(info[17]),
            Double.parseDouble(info[18]), Double.parseDouble(info[19]),
            Double.parseDouble(info[20]),
            Double.parseDouble(info[20]), Double.parseDouble(info[21]));
        courses.add(course);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  //1
  public Map<String, Integer> getPtcpCountByInst() {
    return courses.stream().collect(
        Collectors.groupingBy(
            Course::getInstitution,
            TreeMap::new,
            Collectors.summingInt(Course::getParticipants)
        )
    );
  }

  //2
  public Map<String, Integer> getPtcpCountByInstAndSubject() {
    Map<String, Integer> unsortedMap =  courses.stream().collect(
        Collectors.groupingBy(
            course -> course.getInstitution() + '-' + course.getSubject(),
            Collectors.summingInt(Course::getParticipants)
        )
    );
    Map<String, Integer> sortedMap = unsortedMap.entrySet().stream()
        .sorted(Comparator.comparingInt((Entry<String, Integer> c) -> c.getValue())
            .reversed().thenComparing(Entry::getKey))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    return sortedMap;
  }

  //3
  public Map<String, List<List<String>>> getCourseListOfInstructor() {
    Map<String, List<Set<String>>> res = new HashMap<>();
    courses.forEach(course -> {
      boolean isIndependentCourse = course.getInstructors().length == 1;

      for (final String instructor : course.getInstructors()) {
        if (!res.containsKey(instructor)) {
          List<Set<String>> list = new ArrayList<>();
          list.add(new HashSet<>());
          list.add(new HashSet<>());
          res.put(instructor, list);
        }
        List<Set<String>> outerList = res.get(instructor);
        Set<String> coursesList = isIndependentCourse ? outerList.get(0) : outerList.get(1);

        coursesList.add(course.getTitle());
      }
    });
    Map<String, List<List<String>>> ret = res.entrySet().stream()
        .collect(Collectors.toMap(Entry::getKey, l -> {
          List<List<String>> listOfList = new ArrayList<>();
          List<Set<String>> listOfSet = l.getValue();
          listOfList.add(listOfSet.get(0).stream().sorted(Comparator.naturalOrder()).toList());
          listOfList.add(listOfSet.get(1).stream().sorted(Comparator.naturalOrder()).toList());
          return listOfList;
        }));
    return ret;
  }

  //4
  public List<String> getCourses(int topK, String by) {

    if (by.equals("hours")) {

      return courses.stream().collect(Collectors.toMap(
              Course::getTitle,
              Function.identity(),
              BinaryOperator.maxBy(Comparator.comparingDouble(Course::getTotalHours))
          )).entrySet().stream().map(entry -> entry.getValue())
          .sorted(
              Comparator.comparingDouble(Course::getTotalHours).reversed()
                  .thenComparing(Course::getTitle)
          )
          .limit(topK).map(Course::getTitle)
          .toList();
    } else {
      return courses.stream().collect(Collectors.toMap(
              Course::getTitle,
              Function.identity(),
              BinaryOperator.maxBy(Comparator.comparingInt(Course::getParticipants))
          )).entrySet().stream().map(entry -> entry.getValue())
          .sorted(
              Comparator.comparingInt(Course::getParticipants).reversed()
                  .thenComparing(Course::getTitle)
          )
          .limit(topK).map(Course::getTitle)
          .toList();
    }
  }

  //5
  public List<String> searchCourses(String courseSubject, double percentAudited, double totalCourseHours) {
    String finalCourseSubject = courseSubject.toLowerCase();
    return courses.stream().filter(
            c -> c.getPercentAudited() >= percentAudited
                && c.getTotalHours() <= totalCourseHours
                && c.getSubject().toLowerCase().contains(finalCourseSubject)
        ).map(Course::getTitle).collect(Collectors.toSet())
        .stream().sorted(Comparator.naturalOrder()).toList();
  }

  //6
  public List<String> recommendCourses(int age, int gender, int isBachelorOrHigher) {
    Map<String, List<Course>> coursesById = courses.stream()
        .collect(Collectors.groupingBy(Course::getNumber));
    Map<String, Double> courseSimilarty = coursesById.entrySet().stream().collect(Collectors.toMap(
        (Entry<String, List<Course>> entry) -> {
          List<Course> list = entry.getValue();
          list.sort(Comparator.comparing(Course::getLaunchDate).reversed());
          return list.get(0).getTitle();
        },
        (Entry<String, List<Course>> entry) -> {
          List<Course> list = entry.getValue();
          double avgMedianAge = list.stream().mapToDouble(Course::getMedianAge).average().getAsDouble();
          double avgMale = list.stream().mapToDouble(Course::getPercentMale).average().getAsDouble();
          double avgDegree = list.stream().mapToDouble(Course::getPercentDegree).average().getAsDouble();
          return Math.pow(age - avgMedianAge, 2) + Math.pow(gender * 100 - avgMale, 2) + Math.pow(isBachelorOrHigher * 100 - avgDegree, 2);
        },
        (x, y) -> Math.min(x, y)
    ));
    return courseSimilarty.entrySet().stream()
        .sorted(Entry.<String, Double>comparingByValue()
            .thenComparing((Entry<String, Double> entry) -> entry.getKey()))
        .limit(10)
        .map(Entry::getKey)
        .toList();
  }

}

class Course {
  String institution;
  String number;
  Date launchDate;
  String title;

  String[] instructors;
  String subject;
  int year;
  int honorCode;
  int participants;
  int audited;
  int certified;
  double percentAudited;
  double percentCertified;
  double percentCertified50;
  double percentVideo;
  double percentForum;
  double gradeHigherZero;
  double totalHours;
  double medianHoursCertification;
  double medianAge;
  double percentMale;
  double percentFemale;
  double percentDegree;

  public Date getLaunchDate() {
    return launchDate;
  }

  public double getMedianAge() {
    return medianAge;
  }

  public double getPercentMale() {
    return percentMale;
  }

  public double getPercentDegree() {
    return percentDegree;
  }

  public String getNumber() {
    return number;
  }

  public double getPercentAudited() {
    return percentAudited;
  }

  public double getTotalHours() {
    return totalHours;
  }

  public String getTitle() {
    return title;
  }

  public int getParticipants() {
    return participants;
  }

  public String  getSubject() {
    return subject;
  }

  public String getInstitution() {
    return institution;
  }

  public String[] getInstructors() {
    return instructors;
  }


  public Course(String institution, String number, Date launchDate,
                String title, String instructors, String subject,
                int year, int honorCode, int participants,
                int audited, int certified, double percentAudited,
                double percentCertified, double percentCertified50,
                double percentVideo, double percentForum, double gradeHigherZero,
                double totalHours, double medianHoursCertification,
                double medianAge, double percentMale, double percentFemale,
                double percentDegree) {
    this.institution = institution;
    this.number = number;
    this.launchDate = launchDate;
    if (title.startsWith("\"")) {
      title = title.substring(1);
    }
    if (title.endsWith("\"")) {
      title = title.substring(0, title.length() - 1);
    }
    this.title = title;
    if (instructors.startsWith("\"")) {
      instructors = instructors.substring(1);
    }
    if (instructors.endsWith("\"")) {
      instructors = instructors.substring(0, instructors.length() - 1);
    }
    this.instructors = instructors.split(", ");
    if (subject.startsWith("\"")) {
      subject = subject.substring(1);
    }
    if (subject.endsWith("\"")) {
      subject = subject.substring(0, subject.length() - 1);
    }
    this.subject = subject;
    this.year = year;
    this.honorCode = honorCode;
    this.participants = participants;
    this.audited = audited;
    this.certified = certified;
    this.percentAudited = percentAudited;
    this.percentCertified = percentCertified;
    this.percentCertified50 = percentCertified50;
    this.percentVideo = percentVideo;
    this.percentForum = percentForum;
    this.gradeHigherZero = gradeHigherZero;
    this.totalHours = totalHours;
    this.medianHoursCertification = medianHoursCertification;
    this.medianAge = medianAge;
    this.percentMale = percentMale;
    this.percentFemale = percentFemale;
    this.percentDegree = percentDegree;
  }
}