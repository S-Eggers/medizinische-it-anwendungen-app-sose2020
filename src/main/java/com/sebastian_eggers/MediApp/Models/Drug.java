package com.sebastian_eggers.MediApp.Models;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.sebastian_eggers.MediApp.Enum.DrugForm;
import com.sebastian_eggers.MediApp.Enum.NotificationRepeat;
import com.sebastian_eggers.MediApp.R;
import com.sebastian_eggers.MediApp.Receiver.NotificationReceiver;
import com.sebastian_eggers.MediApp.Util.NotificationUtil;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

public class Drug implements Comparable<Drug>, Serializable {
    private transient long id;
    private String name;
    private String description;
    private ArrayList<LocalTime> intake;
    private ArrayList<DayOfWeek> days;
    private LocalDate dateOfLastIntake;
    private int dosePerIntake;
    private String doseUnit;
    private String form;
    private long lastIntake;

    public Drug(String name, ArrayList<LocalTime> time, ArrayList<DayOfWeek> days, int dosePerIntake, DrugForm drugForm) {
        this.name = name;
        this.intake = time;
        this.days = days;
        this.dosePerIntake = dosePerIntake;
        this.form = drugForm.toString();
        this.id = -1;
        this.lastIntake = 0;
        this.dateOfLastIntake = null;
    }

    public Drug(String name, ArrayList<LocalTime> time, ArrayList<DayOfWeek> days, int dosePerIntake, DrugForm drugForm, String description) {
        this(name, time, days, dosePerIntake, drugForm);
        this.description = description;
    }

    public Drug(String name, ArrayList<LocalTime> time, ArrayList<DayOfWeek> days, int dosePerIntake, DrugForm drugForm, String description, String doseUnit) {
        this(name, time, days, dosePerIntake, drugForm, description);
        this.doseUnit = doseUnit;
    }

    /**
     * _____________________________________________________________________________________________
     *
     *                                  Getter & Setter
     *
     * _____________________________________________________________________________________________
     */

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getForm() {
        return form;
    }

    public ArrayList<LocalTime> getIntake() {
        return intake;
    }

    public ArrayList<DayOfWeek> getDays() {
        return days;
    }

    public int getDosePerIntake() {
        return dosePerIntake;
    }

    public String getDoseUnit() {
        return doseUnit;
    }

    public void setDoseUnit(String doseUnit) {
        this.doseUnit = doseUnit;
    }

    public boolean doseUnitExists() {
        return this.doseUnit != null;
    }

    public boolean descriptionExists() {
        return this.description != null;
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        if (this.id == -1)
            this.id = id;
        else
            throw new RuntimeException("You are not allowed to set a Drug ID after it's initially set.");
    }

    public long getLastIntake() {
        return this.lastIntake;
    }

    public void setLastIntake(long intake) {
        this.lastIntake = intake;
    }

    public void setDateOfLastIntake(LocalDate date) {
        dateOfLastIntake = date;
    }

    public LocalDate getDateOfLastIntake() {
        return dateOfLastIntake;
    }

    /**
     * _____________________________________________________________________________________________
     *
     *                                  Class functions
     *
     * _____________________________________________________________________________________________
     */

    /**
     * Returns whether an intake is today after the current time
     * @return true if intake is after current time else false
     */
    public boolean isNextIntakeToday() {
        return isNextIntakeToday(LocalDate.now(), LocalTime.now());
    }

    /**
     * Returns whether an intake is at a specific date after a specific time
     * @param localDate The date
     * @param of The time
     * @return true if intake is after specific time at the specific date else false
     */
    public boolean isNextIntakeToday(LocalDate localDate, LocalTime of) {
        DayOfWeek dayOfWeek = localDate.getDayOfWeek();

        boolean found = false;
        for (DayOfWeek day: days) {
            if(dayOfWeek == day) {
                found = true;
                break;
            }
        }
        if(!found) return false;

        return nextIntake(of).compareTo(LocalTime.of(23, 59, 59)) < 0;
    }

    /**
     * Returns the next intake after now
     * @return The next intake
     */
    public LocalTime nextIntake() {
        return nextIntake(LocalTime.now());
    }

    /**
     * Return the next intake after a specific time
     * @param localTime The time
     * @return The next intake
     */
    public LocalTime nextIntake(LocalTime localTime) {
        LocalTime min = LocalTime.of(23, 59, 59);

        for (LocalTime time : intake) {
            if (localTime.compareTo(time) <= 0 && min.compareTo(time) >= 0) {
                min = time;
            }
        }
        return min;
    }

    @Override
    public int compareTo(Drug compareDrug) {
        if(compareDrug.isNextIntakeToday() && !isNextIntakeToday())
            return 1;
        else if(!compareDrug.isNextIntakeToday() && isNextIntakeToday())
            return -1;
        else if(compareDrug.isNextIntakeToday() && isNextIntakeToday())
            return nextIntake().compareTo(compareDrug.nextIntake());

        ArrayList<DayOfWeek> days = getDays();
        Collections.sort(days);
        ArrayList<DayOfWeek> compareDays = compareDrug.getDays();
        Collections.sort(compareDays);
        return days.get(0).compareTo(compareDays.get(0));
    }

    /**
     * Schedules the notifications for this drug
     * @param context Current context
     */
    public void scheduleNotification(Context context) {
        if(dateOfLastIntake != null && dateOfLastIntake.isBefore(LocalDate.now())) return;

        Calendar now = Calendar.getInstance();
        for(LocalTime time: intake) {
            if(days.size() == 7) {
                Notification notification = getNotification(context);
                Calendar alarm = buildAlarmCalendar(time);
                if(now.after(alarm))
                    alarm.add(Calendar.DATE, 1);
                NotificationUtil.scheduleNotification(context, notification, alarm, NotificationRepeat.DAILY);
            }
            else {
                for(DayOfWeek day: days) {
                    Notification notification = getNotification(context);
                    Calendar alarm = buildAlarmCalendar(time);
                    if(diffBetweenDayOfWeek(day) != 0)
                        alarm.add(Calendar.DATE, diffBetweenDayOfWeek(day));
                    NotificationUtil.scheduleNotification(context, notification, alarm, NotificationRepeat.WEEKLY);
                }
            }
        }
    }

    /**
     * Calculates the difference between the current day of week and a given day of week
     * @param day Day of week
     * @return Difference as an integer, starting from 0 if no difference and the intake is today and ending with 7 if no difference but the intake is next week
     */
    private int diffBetweenDayOfWeek(DayOfWeek day) {
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        if(today.compareTo(day) == 0 && !isNextIntakeToday()) return 7;

        int todayValue = today.getValue();
        int dayValue = day.getValue();
        int diff = todayValue - dayValue;

        if(diff > 0) {
            dayValue += 7;
            return dayValue - todayValue;
        }
        else {
            return -1 * diff;
        }
    }

    /**
     * Cancel a notification for this drug
     * @param context The current context
     */
    public void cancelNotifications(Context context)  {
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        NotificationUtil.cancelNotification(context, pendingIntent);
    }

    /**
     * Build a calender based on a given time
     * @param time Time on which the calender should be build
     * @return Calender which contains the given time
     */
    private Calendar buildAlarmCalendar(LocalTime time) {
        Calendar alarm = Calendar.getInstance();
        alarm.set(Calendar.HOUR_OF_DAY, time.getHour());
        alarm.set(Calendar.MINUTE, time.getMinute());
        alarm.set(Calendar.SECOND, time.getSecond());
        return alarm;
    }

    /**
     * Returns the notification of this drug
     * @param context The current context
     * @return Notification for this drug
     */
    private Notification getNotification(Context context) {
        return NotificationUtil.buildNotification(context,
                context.getResources().getString(R.string.app_name),
                context.getResources().getString(R.string.intake_notification).replace("{-}", name));
    }
}
