import {
    SET_CLUSTER_STATE
} from './RootActions';

export const sessionInitialState = {
    clusterState: {},
};

export const reducer = (state = sessionInitialState, action: any) => {
    switch (action.type) {
        case SET_CLUSTER_STATE:
            return Object.assign({}, state, { clusterState: action.state });
    }
    return state;
};