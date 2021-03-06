package com.grpcvsrest.grpc.voting;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.grpcvsrest.grpc.LeaderboardServiceGrpc.LeaderboardServiceFutureStub;
import com.grpcvsrest.grpc.*;
import com.grpcvsrest.grpc.ResponseTypeServiceGrpc.ResponseTypeServiceFutureStub;
import com.grpcvsrest.grpc.VotingServiceGrpc.VotingServiceImplBase;
import io.grpc.stub.StreamObserver;

import javax.annotation.Nullable;

public class VotingService extends VotingServiceImplBase {

    private final ResponseTypeServiceFutureStub responseTypeServiceClient;
    private final LeaderboardServiceFutureStub leaderboardServiceClient;

    public VotingService(ResponseTypeServiceFutureStub responseTypeServiceClient,
                         LeaderboardServiceFutureStub leaderboardServiceClient) {
        this.responseTypeServiceClient = responseTypeServiceClient;
        this.leaderboardServiceClient = leaderboardServiceClient;
    }

    @Override
    public void vote(VotingRequest request, StreamObserver<VotingResponse> responseObserver) {

        ResponseTypeRequest respTypeRequest = ResponseTypeRequest.newBuilder()
                .setAggrItemId(request.getItemId()).build();

        ListenableFuture<ResponseTypeResponse> checkFuture = responseTypeServiceClient.getResponseType(respTypeRequest);

        ListenableFuture<RecordVoteResponse> voteResponse = Futures.transformAsync(checkFuture, responseType -> {
            ResponseType rightCategory = responseType.getType();
            boolean rightGuess = rightCategory == request.getVotedCategory();

            RecordVoteRequest recordVoteRequest = RecordVoteRequest.newBuilder()
                    .setUsername(request.getUsername())
                    .setVotedCategory(rightCategory)
                    .setRightGuess(rightGuess)
                    .build();
            return leaderboardServiceClient.recordVote(recordVoteRequest);
        });

        Futures.addCallback(voteResponse, new FutureCallback() {

            @Override
            public void onSuccess(@Nullable Object result) {
                responseObserver.onNext(VotingResponse.getDefaultInstance());
                responseObserver.onCompleted();
            }

            @Override
            public void onFailure(Throwable t) {
                responseObserver.onError(t);
            }
        });

    }
}
