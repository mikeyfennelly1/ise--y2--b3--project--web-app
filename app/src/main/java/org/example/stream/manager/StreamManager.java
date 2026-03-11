package org.example.stream.manager;

import org.example.stream.exception.InvalidStreamNameException;
import org.example.stream.exception.StreamAlreadyExistsException;
import org.example.stream.exception.StreamNotFoundException;
import org.example.libb3project.dto.TimeSeriesMessageDTO;
import reactor.core.publisher.Flux;

import java.util.List;

public interface StreamManager {
    void createStream(String name, String parent) throws StreamNotFoundException, InvalidStreamNameException, StreamAlreadyExistsException;
    void createAndManageStream(String name);
    void deleteStream(String subscriptionName) throws StreamNotFoundException;
    List<String> getAllStreamNames();
    List<String> getChildStreams(String parentPath) throws StreamNotFoundException, InvalidStreamNameException;
    Flux<TimeSeriesMessageDTO> getStreamSSESink(String streamName);
    boolean streamAlreadyExists(String streamName);
}
