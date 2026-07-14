package com.socials.extractor.browser.event;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class InMemoryEventRecorder
        implements EventRecorder {

    private final List<BrowserEvent> events =
            new CopyOnWriteArrayList<>();

    @Override
    public void record(
            BrowserEvent event
    ) {

        events.add(event);

    }

    public List<BrowserEvent> snapshot() {

        return List.copyOf(events);

    }

}