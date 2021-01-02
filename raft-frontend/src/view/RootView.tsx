import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';

import { makeStyles } from '@material-ui/core/styles';
import { Paper, Grid, Typography } from '@material-ui/core';
import Button from '@material-ui/core/Button';
import TextField from '@material-ui/core/TextField';

import Leader from './Leader';
import Followers from './Followers';
import Candidates from './Candidates';

import { getClusterState, replicateValue, updateValueToReplicate } from '../architecture/RootActions';

const useStyles = makeStyles((theme) => ({
    root: {
        flexGrow: 1,
        padding: '16px'
    },
    paper: {
        padding: theme.spacing(2),
        textAlign: 'left',
        color: theme.palette.text.secondary,
    },
}));

export default function RootView() {
    const valueToReplicate = useSelector((state: any) => state.valueToReplicate);
    const clusterState = useSelector((state: any) => state.clusterState);
    const classes = useStyles();
    const dispatch = useDispatch();

    useEffect(() => {
        dispatch(getClusterState());
    });

    if (clusterState.leader === undefined) {
        return (<Typography
            gutterBottom
            variant='subtitle1'
            align='center'
            style={{ marginTop: '16px' }}>
            Starting cluster...
        </Typography>);
    }

    return (
        <div className={classes.root}>
            <Grid container spacing={3}>
                <Grid item xs={12}>
                    <Paper className={classes.paper}>
                        <TextField
                            id="standard-basic"
                            label="Value"
                            value={valueToReplicate}
                            onChange={e => dispatch(updateValueToReplicate(e.target.value))} />
                        <Button
                            variant="contained"
                            color="primary"
                            onClick={() => {
                                if (valueToReplicate !== "") {
                                    dispatch(replicateValue())
                                }
                            }}>Replicate</Button>
                    </Paper>
                </Grid>
                <Grid item xs>
                    <Paper className={classes.paper}>
                        <Leader leader={clusterState.leader} />
                    </Paper>
                </Grid>
                <Grid item xs>
                    <Paper className={classes.paper}>
                        <Followers followers={clusterState.followers} />
                    </Paper>
                </Grid>
                <Grid item xs>
                    <Paper className={classes.paper}>
                        <Candidates candidates={clusterState.candidates} />
                    </Paper>
                </Grid>
            </Grid>
        </div>
    );
}